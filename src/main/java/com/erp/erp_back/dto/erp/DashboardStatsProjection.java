package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

public record DashboardStatsProjection(
    BigDecimal yesterdaySales,
    BigDecimal thisWeekSales,   // 오늘 제외, 이번주 누적(월~어제)
    BigDecimal lastWeekSales,   // 지난주 전체
    BigDecimal thisMonthSales,  // 오늘 제외, 이번달 누적(1일~어제)
    BigDecimal lastMonthSales   // 지난달 전체
) {}