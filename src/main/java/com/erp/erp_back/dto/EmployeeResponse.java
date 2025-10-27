package com.erp.erp_back.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long employeeId;
    private String name;
    private String email;
    private String phone;
    private String provider;
    private LocalDateTime createdAt;
}