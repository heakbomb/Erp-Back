package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long itemId;
    private Long storeId;
    private String itemName;
    private String itemType;    
    private String stockType;  
    private BigDecimal stockQty;
    private BigDecimal safetyQty;
}