package com.erp.erp_back.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransactionResponse {
    private Long transactionId;
    private Long storeId;
    private Long menuId;
    private LocalDateTime transactionTime;
    private BigDecimal salesAmount;
}