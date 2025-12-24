package com.erp.erp_back.dto.subscription;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long subId;
    private String subName;
    private BigDecimal monthlyPrice;
    @JsonProperty("isActive")
    private Boolean isActive;
}