package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;

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
    private Double latitude;
    private Double longitude;

    // ✅ 기존 방식: GPS 없이 Store만으로 만들 때
    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .bizId(store.getBusinessNumber() != null ? store.getBusinessNumber().getBizId() : null)
                .storeName(store.getStoreName())
                .industry(store.getIndustry())
                .posVendor(store.getPosVendor())
                .status(store.getStatus())
                .approvedAt(store.getApprovedAt())
                .build();
    }

    // ✅ 새 방식: Store + StoreGps 두 개를 합쳐서 응답
    public static StoreResponse of(Store store, StoreGps gps) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .bizId(store.getBusinessNumber() != null ? store.getBusinessNumber().getBizId() : null)
                .storeName(store.getStoreName())
                .industry(store.getIndustry())
                .posVendor(store.getPosVendor())
                .status(store.getStatus())
                .approvedAt(store.getApprovedAt())
                .latitude(gps != null ? gps.getLatitude() : null)
                .longitude(gps != null ? gps.getLongitude() : null)
                .build();
    }
}