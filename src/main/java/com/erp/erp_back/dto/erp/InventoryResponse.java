// com.erp.erp_back.dto.erp.InventoryResponse.java
package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.enums.IngredientCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long itemId;
    private Long storeId;
    private String itemName;
    private IngredientCategory itemType; 
    private String stockType;
    private BigDecimal stockQty;
    private BigDecimal safetyQty;
    private ActiveStatus status;
    private BigDecimal lastUnitCost;

}
