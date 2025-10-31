package com.erp.erp_back.service.hr;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.erp.erp_back.dto.hr.DocumentListResponseDto;
import com.erp.erp_back.dto.hr.FileUploadResponse;
import com.erp.erp_back.entity.hr.EmployeeDocument;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.exception.FileNotFoundException;
import com.erp.erp_back.exception.FileStorageException;
import com.erp.erp_back.repository.hr.EmployeeDocumentRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private final Path fileStorageLocation;
    private final EmployeeDocumentRepository documentRepository;
    private final StoreRepository storeRepository;

    public EmployeeDocumentServiceImpl(
            @Value("${file.upload-dir}") String uploadDir,
            EmployeeDocumentRepository documentRepository,
            StoreRepository storeRepository) {

        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.documentRepository = documentRepository;
        this.storeRepository = storeRepository;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("파일을 저장할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    @Override
    @Transactional
    public FileUploadResponse storeFile(MultipartFile file, Long storeId, String docType, LocalDate retentionEndDate) {
        if (file.isEmpty()) {
            throw new FileStorageException("업로드할 파일을 선택해주세요.");
        }

        // 1. Store 엔티티 조회 (Proxy 사용)
        Store store = storeRepository.getReferenceById(storeId);

        // 2. 파일명 고유화 (충돌 방지)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } catch (Exception e) {
            // 확장자가 없는 경우
        }
        String storedFilename = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);

        try {
            // 3. 파일 시스템에 파일 저장
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new FileStorageException(storedFilename + " 파일을 저장하는 중 오류가 발생했습니다.", ex);
        }

        // 4. DB에 메타데이터 저장
        EmployeeDocument document = new EmployeeDocument();
        document.setStore(store);
        document.setDocType(docType);
        document.setRetentionEndDate(retentionEndDate);
        document.setOriginalFilename(originalFilename);
        document.setContentType(file.getContentType());
        document.setFilePath(targetLocation.toString()); // 로컬 절대 경로 저장

        EmployeeDocument savedDoc = documentRepository.save(document);

        return FileUploadResponse.builder()
                .documentId(savedDoc.getDocumentId())
                .docType(savedDoc.getDocType())
                .originalFilename(savedDoc.getOriginalFilename())
                .filePath(savedDoc.getFilePath())
                .retentionEndDate(savedDoc.getRetentionEndDate())
                .build();
    }

    @Override
    public Resource loadFileAsResource(Long documentId) {
        try {
            EmployeeDocument document = getDocument(documentId);
            
            // [신규] 다운로드 시점에도 만료일 체크
            LocalDate today = LocalDate.now();
            if (document.getRetentionEndDate().isBefore(today)) {
                log.warn("만료된 파일(ID: {}) 다운로드 시도 차단됨.", documentId);
                // React가 "만료" 상태를 표시하더라도, 스케줄러가 돌기 전까지 파일은 존재함
                // 따라서 여기서 "파일을 찾을 수 없음"으로 처리하여 다운로드를 막음
                throw new FileNotFoundException("파일 보관 기간이 만료되어 다운로드할 수 없습니다.");
            }

            Path filePath = Paths.get(document.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                // 스케줄러가 파일을 이미 삭제한 경우 (정상)
                throw new FileNotFoundException(document.getOriginalFilename() + " 파일을 찾을 수 없거나 읽을 수 없습니다.");
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("파일 경로 오류가 발생했습니다.", ex);
        }
    }

    @Override
    public EmployeeDocument getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new FileNotFoundException(documentId + " ID의 파일을 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    // [수정] search 파라미터 추가
    public Page<DocumentListResponseDto> getDocumentsByStore(Long storeId, String status, String search, Pageable pageable) {
        
        Page<EmployeeDocument> documentsPage;
        LocalDate today = LocalDate.now();

        // [수정] 검색어가 있는지(null이나 공백이 아닌지) 확인
        boolean hasSearchTerm = search != null && !search.trim().isEmpty();
        String searchTerm = "%" + search + "%"; // LIKE 쿼리용 와일드카드 추가

        if (!hasSearchTerm) {
            // 1. 검색어가 없으면 (기존 로직)
            if ("EXPIRED".equalsIgnoreCase(status)) {
                documentsPage = documentRepository.findAllByStore_StoreIdAndRetentionEndDateBefore(storeId, today, pageable);
            } else if ("ACTIVE".equalsIgnoreCase(status)) {
                documentsPage = documentRepository.findAllByStore_StoreIdAndRetentionEndDateGreaterThanEqual(storeId, today, pageable);
            } else {
                documentsPage = documentRepository.findAllByStore_StoreId(storeId, pageable);
            }
        } else {
            // 2. 검색어가 있으면 (신규 @Query 로직)
            if ("EXPIRED".equalsIgnoreCase(status)) {
                documentsPage = documentRepository.findByStoreIdAndExpiredAndSearch(storeId, today, searchTerm, pageable);
            } else if ("ACTIVE".equalsIgnoreCase(status)) {
                documentsPage = documentRepository.findByStoreIdAndActiveAndSearch(storeId, today, searchTerm, pageable);
            } else { // "ALL"
                documentsPage = documentRepository.findByStoreIdAndSearch(storeId, searchTerm, pageable);
            }
        }
        
        // ... (Entity Page -> DTO Page 변환 로직은 동일)
        return documentsPage.map(doc -> DocumentListResponseDto.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())      
                .originalFilename(doc.getOriginalFilename())
                .retentionEndDate(doc.getRetentionEndDate())
                .build());
    }
}