package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseHistoryUpdateRequest {

    @NotNull(message = "구매 수량은 필수입니다.")
    @Positive(message = "구매 수량은 0보다 커야 합니다.")
    @Digits(integer = 7, fraction = 3, message = "구매 수량은 소수 {fraction}자리까지 입력하세요.")
    private BigDecimal purchaseQty;

    @NotNull(message = "단가는 필수입니다.")
    @Positive(message = "단가는 0보다 커야 합니다.")
    @Digits(integer = 8, fraction = 2, message = "단가는 소수 {fraction}자리까지 입력하세요.")
    private BigDecimal unitPrice;

    @NotNull(message = "구매일은 필수입니다.")
    @PastOrPresent(message = "구매일은 오늘 이후일 수 없습니다.")
    private LocalDate purchaseDate;
}