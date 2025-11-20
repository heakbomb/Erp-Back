package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // ⭐️ Setter 필수

@Getter
@Setter
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

    // --- 상세 정보 필드 ---
    private String ownerName;
    private String ownerEmail;
    private String bizNum; 
    private String openStatus; 
    private String taxType; 
    private String startDt; 
    private String phone;

    // --- 직원 목록 리스트 ---
    private List<StoreEmployeeDto> employees;

    @Getter @Builder
    public static class StoreEmployeeDto {
        private String name;
        private String role;
        private String status;
    }

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