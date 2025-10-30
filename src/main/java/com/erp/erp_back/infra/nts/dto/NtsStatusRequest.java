package com.erp.erp_back.infra.nts.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// 요청 바디: {"b_no":["1234567890", ...]}
public record NtsStatusRequest(
        @JsonProperty("b_no") List<String> bNo
) {}