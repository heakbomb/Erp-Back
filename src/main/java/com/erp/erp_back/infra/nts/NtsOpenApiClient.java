// src/main/java/com/erp/erp_back/infra/nts/NtsOpenApiClient.java
package com.erp.erp_back.infra.nts;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.erp.erp_back.infra.nts.dto.NtsStatusRequest;
import com.erp.erp_back.infra.nts.dto.NtsStatusResponse;

@Component
public class NtsOpenApiClient {

    private final String baseUrl;
    private final String serviceKey;
    private final RestTemplate restTemplate;

    public NtsOpenApiClient(
            @Value("${nts.base-url}") String baseUrl,
            @Value("${nts.service-key}") String serviceKey,
            RestTemplate restTemplate
    ) {
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
        this.restTemplate = restTemplate;
    }

    /** 국세청 사업자 상태조회 호출 */
    public NtsStatusResponse status(List<String> bizNumbers) {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl.trim())
                .path("/status")
                .queryParam("serviceKey", serviceKey) // ✅ 문자열 붙이지 말고 queryParam 사용
                .queryParam("returnType", "JSON")
                .build(true) // 이미 인코딩된 serviceKey도 최대한 존중
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NtsStatusRequest> entity =
                new HttpEntity<>(new NtsStatusRequest(bizNumbers), headers);

        try {
            ResponseEntity<NtsStatusResponse> res = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    entity,
                    NtsStatusResponse.class
            );

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new IllegalStateException("NTS API 비정상 응답: " + res.getStatusCode());
            }
            return res.getBody();

        } catch (HttpStatusCodeException e) {
            // ✅ 외부가 준 status/body를 그대로 포함 (원인 파악용)
            throw new IllegalStateException(
                    "NTS API 실패: status=" + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}
