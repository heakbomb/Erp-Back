package com.erp.erp_back.infra.nts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 응답 data 배열의 원소
public record NtsStatusItem(
        @JsonProperty("b_no") String bNo,
        @JsonProperty("b_stt") String bStt,
        @JsonProperty("tax_type") String taxType,
        @JsonProperty("start_dt") String startDt,
        @JsonProperty("end_dt") String endDt
) {}