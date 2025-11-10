// com.erp.erp_back.dto.erp.MenuItemResponse.java
package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import com.erp.erp_back.entity.enums.ActiveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {
    private Long menuId;
    private Long storeId;
    private String menuName;
    private BigDecimal price;
    private BigDecimal calculatedCost;
    private ActiveStatus status;  
}
