package com.erp.erp_back.dto.erp;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeDocumentRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long storeId; 

    @NotBlank
    @Size(max = 50)
    private String docType; // (예: "근로계약서", "통장사본")

    @NotBlank
    @Size(max = 255)
    private String filePath; // (S3 업로드 후 반환된 경로)

    @NotNull
    private LocalDate retentionEndDate; 
}
