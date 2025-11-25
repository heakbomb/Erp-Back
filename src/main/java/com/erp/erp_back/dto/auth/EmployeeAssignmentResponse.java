package com.erp.erp_back.dto.auth;

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
public class EmployeeAssignmentResponse {
    private Long assignmentId;
    private Long employeeId;
    private Long storeId;
    private String role;
    private String status; 
}