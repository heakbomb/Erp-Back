package com.erp.erp_back.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.erp_back.entity.enums.StoreIndustry; // Enum Import

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor 
@NoArgsConstructor
public class AdminStoreDashboardItem {
    private Long storeId;
    private String storeName;
    
    // ✅ [수정] String -> StoreIndustry
    private StoreIndustry industry;
    
    private String status;
    private long employeeCount; 
    
    // ✅ [수정] 필드명 변경 (Mapper, Repository와 통일)
    private BigDecimal totalSales;       // 기존 totalSalesMonth
    private LocalDateTime lastTransaction; // 기존 lastSalesDate

    private String ownerName;
    private String ownerEmail;
    private String bizNum; 
}