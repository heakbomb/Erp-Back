package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransactionSummaryResponse {

    private Long transactionId;
    private String transactionTime;
    private String paymentMethod;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal totalDiscount;
    private String itemsSummary;
}