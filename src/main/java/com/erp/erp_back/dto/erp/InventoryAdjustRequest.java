package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryAdjustRequest {

    @NotNull(message = "deltaQty는 필수입니다.")        // 증감 수량(양수=입고, 음수=차감)
    @Digits(integer = 10, fraction = 3)                 // 스키마 scale=3 맞춤
    private BigDecimal deltaQty;

}