package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.erp_back.entity.enums.StoreIndustry; // Enum Import

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private Long storeId;
    private Long bizId;
    private String storeName;
    
    // ✅ [수정] String -> StoreIndustry
    private StoreIndustry industry;
    
    private String posVendor;
    private String status;
    private LocalDateTime approvedAt;
    private Double latitude;
    private Double longitude;
    private Boolean active;

    // --- 상세 정보 필드 ---
    private String ownerName;
    private String ownerEmail;
    private String bizNum; 
    private String openStatus; 
    private String taxType; 
    private String startDt; 
    private String phone;

    private List<StoreEmployeeDto> employees;

    @Getter
    @Setter 
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreEmployeeDto {
        private String name;
        private String role;
        private String status;
    }
}