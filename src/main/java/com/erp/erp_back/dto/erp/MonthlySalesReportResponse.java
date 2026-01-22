package com.erp.erp_back.dto.erp;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 월간 매출 리포트 전체 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySalesReportResponse {

    private MonthlySalesSummaryResponse summary;            // 지난달/이번달/증감액
    private List<TopMenuStatsResponse> topMenus;            // 인기 메뉴 비율
    private List<WeeklySalesPointResponse> weeklySales;     // 주간 매출 그래프 + 표
}
