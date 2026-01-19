package com.erp.erp_back.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    // 1. RestClient 빈 등록 (AiDataService용)
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    // 2. RestTemplate 빈 등록 (NtsOpenApiClient용 - ✅ 추가된 부분)
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}