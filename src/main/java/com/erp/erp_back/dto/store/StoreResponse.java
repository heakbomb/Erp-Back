package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

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

    // ✅ Store 엔티티 → DTO 변환용 생성자
    public StoreResponse(Store store) {
        this.storeId = store.getStoreId();
        this.bizId = store.getBusinessNumber() != null ? store.getBusinessNumber().getBizId() : null;
        this.storeName = store.getStoreName();
        this.industry = store.getIndustry();
        this.posVendor = store.getPosVendor();
        this.status = store.getStatus();
        this.approvedAt = store.getApprovedAt();
    }

    // ✅ 정적 팩토리 메서드 (Service에서 사용)
    public static StoreResponse from(Store store) {
        return new StoreResponse(store);
    }
}