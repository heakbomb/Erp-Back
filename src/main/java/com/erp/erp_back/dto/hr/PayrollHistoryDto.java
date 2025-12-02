package com.erp.erp_back.dto.hr;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollHistoryDto {

    private String month;      // "2025-11"
    private long totalPaid;    // 총 지급액
    private int employees;     // 직원 수
    private String status;     // "완료" / "예정" 등
}