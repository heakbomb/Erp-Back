package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosOrderRequest {

    @NotNull
    private Long storeId;

    private String idempotencyKey;

    @NotNull
    private String paymentMethod; // "CARD", "CASH" 등 문자열

    @NotNull
    private BigDecimal totalDiscount;

    @Valid
    @NotEmpty
    private List<PosOrderLine> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PosOrderLine {
        @NotNull
        private Long menuId;
        @NotNull
        private Integer quantity;
        @NotNull
        private BigDecimal unitPrice;
    }
}
