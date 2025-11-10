package com.erp.erp_back.dto.subscription;

import java.time.LocalDate;

import com.erp.erp_back.entity.subscripition.OwnerSubscription;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOwnerSubscriptionResponse {
    // OwnerSubscription 기본 정보
    private Long ownerSubId;
    private LocalDate startDate;
    private LocalDate expiryDate;

    // Owner 정보
    private Long ownerId;
    private String ownerName; // (Owner.username)
    private String ownerEmail;

    // Subscription 정보
    private Long subId;
    private String subName;

    // DTO 변환 헬퍼
    public static AdminOwnerSubscriptionResponse from(OwnerSubscription os) {
        return AdminOwnerSubscriptionResponse.builder()
                .ownerSubId(os.getOwnerSubId())
                .startDate(os.getStartDate())
                .expiryDate(os.getExpiryDate())
                .ownerId(os.getOwner().getOwnerId())
                .ownerName(os.getOwner().getUsername())
                .ownerEmail(os.getOwner().getEmail())
                .subId(os.getSubscription().getSubId())
                .subName(os.getSubscription().getSubName())
                .build();
    }
}