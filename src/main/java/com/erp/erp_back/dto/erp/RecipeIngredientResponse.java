package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientResponse {
    private Long recipeId;
    private Long menuId;
    private Long itemId;
    private BigDecimal consumptionQty;
}