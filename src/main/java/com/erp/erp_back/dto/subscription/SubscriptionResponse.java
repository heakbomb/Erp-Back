package com.erp.erp_back.dto.subscription;

import java.math.BigDecimal;

import com.erp.erp_back.entity.subscripition.Subscription;

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
    private boolean isActive;

    /**
     * Entity -> DTO 변환 헬퍼
     */
    public static SubscriptionResponse fromEntity(Subscription s) {
        return SubscriptionResponse.builder()
                .subId(s.getSubId())
                .subName(s.getSubName())
                .monthlyPrice(s.getMonthlyPrice())
                .isActive(s.isActive())
                .build();
    }
}