package com.erp.erp_back.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private Boolean isActive;

    // [추가] 해지 여부 및 사유 전달
    private boolean canceled; 
    private String cancelReason;
}