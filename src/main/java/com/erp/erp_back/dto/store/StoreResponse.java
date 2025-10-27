package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private Long storeId;
    private Long bizId;
    private String storeName;
    private String industry;
    private String posVendor;
    private String status; 
    private LocalDateTime approvedAt; 
}