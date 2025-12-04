package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지난달 / 이번달 / 증감액 요약
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySalesSummaryResponse {

    private BigDecimal lastMonthTotal;  // 지난달 총 매출
    private BigDecimal thisMonthTotal;  // 이번달 총 매출
    private BigDecimal diff;            // 전월 대비 증감액
}
