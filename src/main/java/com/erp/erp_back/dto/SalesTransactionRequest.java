package com.erp.erp_back.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SalesTransactionRequest {

    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;

    @NotNull
    private Long storeId; 

    @NotNull
    private Long menuId; 

    @NotNull
    private LocalDateTime transactionTime; 

    @NotNull
    @Positive
    private BigDecimal salesAmount; 
}