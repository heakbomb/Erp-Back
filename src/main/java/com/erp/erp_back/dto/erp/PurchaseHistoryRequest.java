package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseHistoryRequest {

    @NotNull
    private Long storeId; 

    @NotNull
    private Long itemId; 

    @NotNull
    @Positive
    private BigDecimal purchaseQty;

    @NotNull
    @PositiveOrZero
    private BigDecimal unitPrice; 

    @NotNull
    @PastOrPresent
    private LocalDate purchaseDate; 
}