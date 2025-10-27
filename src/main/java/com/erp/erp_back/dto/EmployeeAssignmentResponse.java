package com.erp.erp_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssignmentResponse {
    private Long assignmentId;
    private Long employeeId;
    private Long storeId;
    private String role;
    private String status; 
}