package com.erp.erp_back.service.hr;


import java.time.LocalDate;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.erp.erp_back.dto.hr.DocumentListResponseDto;
import com.erp.erp_back.dto.hr.FileUploadResponse;
import com.erp.erp_back.entity.hr.EmployeeDocument;

public interface EmployeeDocumentService {
    /**
     * 파일 업로드 및 DB 메타데이터 저장
     */
    FileUploadResponse storeFile(MultipartFile file, Long storeId, String docType, LocalDate retentionEndDate);

    /**
     * 파일 리소스 로드 (다운로드용)
     */
    Resource loadFileAsResource(Long documentId);

    /**
     * 파일 메타데이터 조회 (다운로드 시 원본 파일명 확인용)
     */
    EmployeeDocument getDocument(Long documentId);

    /**
     * [신규] 사업장별 문서 목록 조회
     */
    Page<DocumentListResponseDto> getDocumentsByStore(Long storeId, String status, String search, Pageable pageable);
    
}
