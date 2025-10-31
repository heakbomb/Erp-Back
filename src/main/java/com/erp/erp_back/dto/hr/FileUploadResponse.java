package com.erp.erp_back.dto.hr;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FileUploadResponse {
    private Long documentId;
    private String docType;
    private String originalFilename;
    private String filePath;
    private LocalDate retentionEndDate;
}
