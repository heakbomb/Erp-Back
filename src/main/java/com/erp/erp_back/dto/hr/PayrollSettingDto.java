// src/main/java/com/erp/erp_back/dto/hr/PayrollSettingDto.java
package com.erp.erp_back.dto.hr;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSettingDto {

    private Long settingId;      // null 이면 신규
    private Long employeeId;
    private String employeeName; // 조회용
    private String role;         // STAFF / MANAGER 등 (EmployeeAssignment.role)

    private String wageType;     // "HOURLY" / "MONTHLY"
    private Long baseWage;       // 표시는 정수원(low), DB에선 decimal(10,2)로 저장
    private Double deductionRate; // 0.033 같은 퍼센트 (간단 버전)
}