// src/main/java/com/erp/erp_back/service/erp/RecipeIngredientService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.RecipeIngredientRequest;
import com.erp.erp_back.dto.erp.RecipeIngredientResponse;
import com.erp.erp_back.dto.erp.RecipeIngredientUpdateRequest;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.mapper.RecipeIngredientMapper;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.common.ErrorCodes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) 
public class RecipeIngredientService {

    private final RecipeIngredientRepository recipeIngredientRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;
    private final MenuItemService menuItemService; 
    private final RecipeIngredientMapper recipeIngredientMapper;

    /** 메뉴별 레시피 목록 */
    @Transactional(readOnly = true)
    public List<RecipeIngredientResponse> listByMenu(Long menuId) {
        Objects.requireNonNull(menuId, ErrorCodes.MENU_ID_MUST_NOT_BE_NULL);

        menuItemRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));

        return recipeIngredientRepository.findByMenuItemMenuId(menuId)
                .stream()
                .map(recipeIngredientMapper::toResponse)
                .toList();
    }

    /** 레시피 등록 */
    @Transactional
    public RecipeIngredientResponse createRecipe(RecipeIngredientRequest req) {
        if (req.getConsumptionQty() == null || req.getConsumptionQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ErrorCodes.INVALID_CONSUMPTION_QTY);
        }

        MenuItem menu = menuItemRepository.findById(req.getMenuId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));

        Inventory inv = inventoryRepository.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));

        // 동일 매장인지 검증
        if (!menu.getStore().getStoreId().equals(inv.getStore().getStoreId())) {
            throw new IllegalArgumentException(ErrorCodes.STORE_MISMATCH_BETWEEN_MENU_AND_INVENTORY);
        }

        // 비활성 사용 금지 정책
        if (menu.getStatus() == ActiveStatus.INACTIVE) {
            throw new IllegalStateException(ErrorCodes.CANNOT_ATTACH_INGREDIENT_TO_INACTIVE_MENU);
        }
        if (inv.getStatus() == ActiveStatus.INACTIVE) {
            throw new IllegalStateException(ErrorCodes.CANNOT_USE_INACTIVE_INVENTORY_IN_RECIPE);
        }

        // 중복 방지
        if (recipeIngredientRepository.existsByMenuItemMenuIdAndInventoryItemId(req.getMenuId(), req.getItemId())) {
            throw new DuplicateKeyException(ErrorCodes.INGREDIENT_ALREADY_EXISTS_FOR_MENU);
        }

        RecipeIngredient saved = recipeIngredientRepository.save(
                RecipeIngredient.builder()
                        .menuItem(menu)
                        .inventory(inv)
                        .consumptionQty(req.getConsumptionQty())
                        .build()
        );

        // ✅ 레시피가 바뀌었으니 해당 메뉴 원가 재계산 + 저장
        menuItemService.recalcAndSave(menu.getStore().getStoreId(), menu.getMenuId());

        return recipeIngredientMapper.toResponse(saved);
    }

    /** 레시피 수정(소모 수량만) */
    @Transactional
    public RecipeIngredientResponse updateRecipe(Long recipeId, RecipeIngredientUpdateRequest req) {
        Objects.requireNonNull(recipeId,ErrorCodes.RECIPE_ID_MUST_NOT_BE_NULL);

        RecipeIngredient entity = recipeIngredientRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.RECIPE_INGREDIENT_NOT_FOUND));

        if (req.getConsumptionQty() == null || req.getConsumptionQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ErrorCodes.INVALID_CONSUMPTION_QTY);
        }

        MenuItem menu = entity.getMenuItem();
        Inventory inv = entity.getInventory();

        if (menu.getStatus() == ActiveStatus.INACTIVE) {
            throw new IllegalStateException(ErrorCodes.CANNOT_MODIFY_RECIPE_OF_INACTIVE_MENU);
        }
        if (inv.getStatus() == ActiveStatus.INACTIVE) {
            throw new IllegalStateException(ErrorCodes.CANNOT_USE_INACTIVE_INVENTORY_IN_RECIPE);
        }

        entity.setConsumptionQty(req.getConsumptionQty()); // 변경감지만

        // ✅ 변경 즉시 해당 메뉴 원가 재계산 + 저장
        menuItemService.recalcAndSave(menu.getStore().getStoreId(), menu.getMenuId());

        return recipeIngredientMapper.toResponse(entity);
    }

    /** 레시피 삭제 */
    @Transactional
    public void deleteRecipe(Long recipeId) {
        Objects.requireNonNull(recipeId, ErrorCodes.RECIPE_ID_MUST_NOT_BE_NULL);

        RecipeIngredient entity = recipeIngredientRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.RECIPE_INGREDIENT_NOT_FOUND));

        Long storeId = entity.getMenuItem().getStore().getStoreId();
        Long menuId  = entity.getMenuItem().getMenuId();

        recipeIngredientRepository.delete(entity);

        // ✅ 삭제 후에도 해당 메뉴 원가 재계산 + 저장
        menuItemService.recalcAndSave(storeId, menuId);
    }

}
