// src/main/java/com/erp/erp_back/dto/hr/OwnerPayrollResponse.java
package com.erp.erp_back.dto.hr;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OwnerPayrollResponse {

    private List<EmployeePayroll> employees;
    private List<PayrollHistoryDto> history;
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeePayroll {
        private Long id;           // 직원 ID
        private String name;       // 이름
        private String role;       // 역할 (STAFF, MANAGER 등)

        private long workDays;     // 근무일수
        private double workHours;  // 근무시간 (시간 단위)

        private long hourlyWage;   // 시급 (지금은 0으로 내려줌)
        private long basePay;      // 기본급
        private long bonus;        // 상여금
        private long deductions;   // 공제액
        private long netPay;       // 실수령액

        private String status;     // "예정" / "지급완료" 등
    }
}