// src/main/java/com/erp/erp_back/dto/erp/PosOrderResponse.java
package com.erp.erp_back.dto.erp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
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
    public static class LineSummary {
        private Long lineId;
        private Long menuId;
        private String menuName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
    }
}
