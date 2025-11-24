package com.erp.erp_back.dto.subscription;

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
}