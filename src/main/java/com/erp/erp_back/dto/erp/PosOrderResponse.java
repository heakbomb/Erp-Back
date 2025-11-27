// src/main/java/com/erp/erp_back/dto/erp/PosOrderResponse.java
package com.erp.erp_back.dto.erp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosOrderResponse {

    private Long transactionId;
    private Long storeId;
    private LocalDateTime transactionTime;

    private BigDecimal totalAmount;
    private BigDecimal totalDiscount;

    private String status;         // PAID, CANCELLED 등
    private String paymentMethod;  // CARD, CASH 등

    private List<LineSummary> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineSummary {
        private Long lineId;
        private Long menuId;
        private String menuName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
    }
}
