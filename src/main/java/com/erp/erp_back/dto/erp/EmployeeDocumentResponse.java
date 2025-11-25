package com.erp.erp_back.dto.erp;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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