// src/main/java/com/erp/erp_back/dto/hr/PayrollCalcResultDto.java
package com.erp.erp_back.dto.hr;

import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollCalcResultDto {

    // 전체 합계
    private long totalWorkMinutes;
    private long totalGrossPay;   // 총 지급액 합계
    private long totalDeductions; // 공제액 합계
    private long totalNetPay;     // 실수령액 합계

    // 직원별 상세 (기존 EmployeePayroll 재사용해도 되고, 별도 DTO 써도 됨)
    private List<OwnerPayrollResponse.EmployeePayroll> employees;
}