package com.erp.erp_back.service.erp.component;

import static com.erp.erp_back.util.BigDecimalUtils.nz;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MenuCostCalculator {
    
    private final RecipeIngredientRepository recipeIngredientRepository;

    public BigDecimal calculate(Long menuId){
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByMenuItemMenuId(menuId);
        BigDecimal sum = BigDecimal.ZERO;

        for(RecipeIngredient ri : ingredients){
            Inventory inv = ri.getInventory();
        
            if(inv == null || inv.getStatus() == ActiveStatus.INACTIVE){
                continue;
            }

            BigDecimal qty = nz(ri.getConsumptionQty());
            BigDecimal last = nz(inv.getLastUnitCost());
            sum = sum.add(qty.multiply(last));
        }
        return sum;
    }
}
