package com.erp.erp_back.dto.erp;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse {

    /* 1. 일간 데이터 (오늘 vs 어제) */
    private BigDecimal todaySales;      // 오늘 매출
    private BigDecimal yesterdaySales;  // 어제 매출 (비교용)

    /* 2. 주간 데이터 (이번주 vs 지난주) */
    private BigDecimal thisWeekSales;   // 이번 주 매출
    private BigDecimal lastWeekSales;   // 지난 주 매출 (비교용)

    /* 3. 월간 데이터 (이번달 vs 지난달) */
    private BigDecimal thisMonthSales;  // 이번 달 매출
    private BigDecimal lastMonthSales;  // 지난 달 매출 (비교용)

    /* 4. 객단가 데이터 (이번달 평균 vs 지난달 평균) */
    private BigDecimal avgTicket;       // 이번 달 객단가
    private BigDecimal prevAvgTicket;   // 지난 달 객단가 (비교용)
}