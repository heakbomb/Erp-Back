package com.erp.erp_back.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocumentResponse {
    private Long documentId;
    private Long employeeId;
    private Long storeId;
    private String docType;
    private String filePath;
    private LocalDate retentionEndDate;
}