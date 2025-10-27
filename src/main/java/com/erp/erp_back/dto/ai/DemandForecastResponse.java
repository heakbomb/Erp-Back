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
    private BigDecimal predictedSalesMax; 
    private Integer predictedVisitors;
}