package com.erp.erp_back.dto.hr;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollHistoryDetailDto {

    private Long payrollId;

    private Long employeeId;
    private String employeeName;
    private String role;          // 필요 없으면 나중에 프론트에서 안 써도 됨

    private String yearMonth;     // "2025-12"

    private long workDays;
    private long workMinutes;

    private long grossPay;        // 총지급액
    private long deductions;      // 공제액
    private long netPay;          // 실수령액

    private String wageType;      // HOURLY / MONTHLY
    private Long baseWage;        // 당시 기준 시급/월급

    private String deductionType; // FOUR_INSURANCE / TAX_3_3 / NONE

    private String status;        // PENDING / PAID
    private LocalDateTime paidAt;
    // 직원 사업장 이름 표시용
    private String storeName; 
}