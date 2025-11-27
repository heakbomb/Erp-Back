package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseHistoryResponse {
    private Long purchaseId;
    private Long storeId;
    private Long itemId;
    private String itemName;
    private BigDecimal purchaseQty;
    private BigDecimal unitPrice;
    private LocalDate purchaseDate;
}