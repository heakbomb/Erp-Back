package com.erp.erp_back.dto.log;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeStatusSummary {
    private long workingCount; // 현재 근무 중
    private long totalCount;   // 전체 직원 (승인된 직원 수)
}
