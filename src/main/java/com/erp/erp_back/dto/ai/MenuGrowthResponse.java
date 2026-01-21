package com.erp.erp_back.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuGrowthResponse {
    private Long menuId;
    private String menuName;
    private Long lastWeekSales;      // 지난주 실제 판매량
    private Long nextWeekPrediction; // 다음주 AI 예측 판매량
    private Double growthRate;       // 증감률 (%)
    private String recommendation;   // 발주 추천 (예: "발주 증량", "유지", "감소")
}