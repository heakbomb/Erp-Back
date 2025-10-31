package com.erp.erp_back.controller.hr;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erp.erp_back.dto.hr.DocumentListResponseDto;
import com.erp.erp_back.dto.hr.FileUploadResponse;
import com.erp.erp_back.entity.hr.EmployeeDocument;
import com.erp.erp_back.service.hr.EmployeeDocumentService;

@RestController
@RequestMapping("/api/hr/documents") // API 경로 예시
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;

    public EmployeeDocumentController(EmployeeDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            // [제거] @RequestParam("employeeId") Long employeeId,
            @RequestParam("storeId") Long storeId,
            @RequestParam("docType") String docType,
            @RequestParam("retentionEndDate") LocalDate retentionEndDate) {

        // [수정] employeeId 파라미터 제거
        FileUploadResponse response = documentService.storeFile(file, storeId, docType, retentionEndDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) throws IOException {
        
        // 1. 파일 메타데이터 조회 (원본 파일명, 타입 확인)
        EmployeeDocument document = documentService.getDocument(documentId);
        
        // 2. 파일 리소스 로드
        Resource resource = documentService.loadFileAsResource(documentId);

        // 3. Content-Disposition 헤더 설정 (한글 파일명 처리)
        String originalFilename = document.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    // [신규] 사업장별 문서 목록 조회 API
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Page<DocumentListResponseDto>> getDocumentsByStore(
            @PathVariable Long storeId,
            @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
            // [신규] search 파라미터 추가, 기본값 "" (빈 문자열)
            @RequestParam(name = "search", defaultValue = "") String search,
            Pageable pageable) {
        
        // 서비스 호출 시 search 전달
        Page<DocumentListResponseDto> documentsPage = documentService.getDocumentsByStore(storeId, status, search, pageable);
        return ResponseEntity.ok(documentsPage);
    }
}