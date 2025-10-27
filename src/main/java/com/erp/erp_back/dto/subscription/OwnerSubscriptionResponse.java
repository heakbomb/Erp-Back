package com.erp.erp_back.dto.subscription;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSubscriptionResponse {
    private Long ownerSubId;
    private Long ownerId;
    private Long subId;
    private LocalDate startDate;
    private LocalDate expiryDate;
}