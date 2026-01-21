package com.erp.erp_back.dto.ai;

public record ProfitForecastResponse(
        Long storeId,
        int year,
        int month,
        String featureYm,
        String predForYm,
        String target,
        long pred,
        String modelPath
) {}
