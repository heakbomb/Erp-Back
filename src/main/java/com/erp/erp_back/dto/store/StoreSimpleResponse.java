package com.erp.erp_back.dto.store;

import com.erp.erp_back.entity.enums.StoreIndustry; // Enum Import

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
    
    // ✅ [수정] String -> StoreIndustry
    private StoreIndustry industry;
    
    private String posVendor;
    private String bizNum;
}