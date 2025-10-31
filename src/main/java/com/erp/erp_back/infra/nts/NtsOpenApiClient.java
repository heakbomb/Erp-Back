package com.erp.erp_back.infra.nts;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.erp.erp_back.infra.nts.dto.NtsStatusRequest;
import com.erp.erp_back.infra.nts.dto.NtsStatusResponse;

@Component
public class NtsOpenApiClient {

    @Value("${nts.base-url}")
    private String baseUrl;

    @Value("${nts.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 국세청 사업자 상태조회 호출 */
    public NtsStatusResponse status(List<String> bizNumbers) {
        // 예: https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=...&returnType=JSON
        String url = String.format("%s/status?serviceKey=%s&returnType=JSON", baseUrl, serviceKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NtsStatusRequest> entity = new HttpEntity<>(
                new NtsStatusRequest(bizNumbers),
                headers
        );

        ResponseEntity<NtsStatusResponse> res = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                NtsStatusResponse.class
        );

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new IllegalStateException("NTS API 호출 실패: " + res.getStatusCode());
        }
        return res.getBody();
    }
}