package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class RecipeIngredientUpdateRequest {
    @NotNull
    @Positive(message = "소모 수량은 0보다 커야 합니다.")
    private BigDecimal consumptionQty;
}