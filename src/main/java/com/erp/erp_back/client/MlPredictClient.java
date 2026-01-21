package com.erp.erp_back.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.erp.erp_back.dto.ai.ProfitForecastResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MlPredictClient {

    private final WebClient webClient;

    @Value("${ml.api.base-url:http://localhost:8001}")
    private String baseUrl;

    public ProfitForecastResponse predictProfit(Long storeId, int year, int month) {
        return webClient
                .get()
                .uri(baseUrl + "/predict/profit?storeId={storeId}&year={year}&month={month}",
                        storeId, year, month)
                .retrieve()
                .bodyToMono(ProfitForecastResponse.class)
                .block();
    }
}
