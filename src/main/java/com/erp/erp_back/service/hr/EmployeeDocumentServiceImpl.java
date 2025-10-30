package com.erp.erp_back.service.hr;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
            Path filePath = Paths.get(document.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
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
    public List<DocumentListResponseDto> getDocumentsByStore(Long storeId) {
        // 1. Repository를 통해 엔티티 목록 조회
        List<EmployeeDocument> documents = documentRepository.findAllByStore_StoreId(storeId);
        
        // 2. Entity List -> DTO List 변환
        return documents.stream()
            .map(doc -> DocumentListResponseDto.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())
                .originalFilename(doc.getOriginalFilename())
                .retentionEndDate(doc.getRetentionEndDate())
                .build())
            .collect(Collectors.toList());
    }
}