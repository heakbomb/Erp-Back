package com.erp.erp_back.dto.hr;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentListResponseDto {
    private Long documentId;
    private String originalFilename;
    private String docType;
    private LocalDate retentionEndDate;
}