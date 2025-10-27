package com.erp.erp_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeAssignmentRequest {

    @NotNull
    private Long employeeId; 
    @NotNull
    private Long storeId; 

    @NotBlank
    @Size(max = 50)
    private String role; 
}