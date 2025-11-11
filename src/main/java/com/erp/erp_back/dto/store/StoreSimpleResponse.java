package com.erp.erp_back.dto.store;

import com.erp.erp_back.entity.store.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class StoreSimpleResponse {
    private Long storeId;
    private String storeName;
    private String status;
    private String industry;
    private String posVendor;

    public static StoreSimpleResponse from(Store s) {
        return StoreSimpleResponse.builder()
                .storeId(s.getStoreId())
                .storeName(s.getStoreName())
                .status(s.getStatus())
                .industry(s.getIndustry())
                .posVendor(s.getPosVendor())
                .build();
    }
}