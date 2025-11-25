package com.erp.erp_back.dto.store;

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
    private String bizNum;
}