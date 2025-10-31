package com.erp.erp_back.infra.nts.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// 전체 응답
public record NtsStatusResponse(
        @JsonProperty("status_code") String statusCode,
        List<NtsStatusItem> data
) {}