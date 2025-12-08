// src/main/java/com/erp/erp_back/dto/hr/EmployeeWorkSummary.java
package com.erp.erp_back.dto.hr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeWorkSummary {

    private Long employeeId;
    private String employeeName;
    private String role;        // EmployeeAssignment 의 역할

    private long workDays;      // 근무일수 (COUNT DISTINCT)
    private long workMinutes;   // 근무시간 (분 단위 합계)
}