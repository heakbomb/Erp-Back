package com.erp.erp_back.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.erp_back.entity.subscripition.OwnerSubscription;
import com.erp.erp_back.entity.subscripition.Subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSubscriptionResponse {
    // OwnerSubscription 정보
    private Long ownerSubId;
    private Long ownerId;
    private LocalDate startDate;
    private LocalDate expiryDate;
    
    // Subscription 상세 정보 (JOIN)
    private Long subId;
    private String subName;
    private BigDecimal monthlyPrice;
    private boolean isActive;

    /**
     * Entity -> DTO 변환 헬퍼
     */
    public static OwnerSubscriptionResponse fromEntity(OwnerSubscription ownerSub) {
        Subscription sub = ownerSub.getSubscription();
        
        return OwnerSubscriptionResponse.builder()
                .ownerSubId(ownerSub.getOwnerSubId())
                .ownerId(ownerSub.getOwner().getOwnerId()) // ⭐️ 
                .startDate(ownerSub.getStartDate())
                .expiryDate(ownerSub.getExpiryDate())
                .subId(sub.getSubId())
                .subName(sub.getSubName())
                .monthlyPrice(sub.getMonthlyPrice())
                .isActive(sub.isActive())
                .build();
    }
}