package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransactionResponse {
    private Long transactionId;
    // storeId 삭제함 (URL 파라미터로 이미 알고 있음)
    
    private LocalDateTime transactionTime;
    private BigDecimal salesAmount;
    private String paymentMethod; // 카드, 현금 등
}