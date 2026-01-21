package com.erp.erp_back.dto.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandForecastResponse {
    private Long forecastId;
    private Long storeId;
    private LocalDate forecastDate; 
    private BigDecimal predictedSalesMax; // 예상 매출 (메뉴별 합산)
    private Integer predictedVisitors;    // 예상 방문객 (수량 기반 추정)
}