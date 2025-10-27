package com.erp.erp_back.dto.subscription;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long subId;
    private String subName;
    private BigDecimal monthlyPrice;
    private Boolean isActive;
}