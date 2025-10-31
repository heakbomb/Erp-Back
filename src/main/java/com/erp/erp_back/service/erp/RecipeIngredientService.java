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
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeIngredientService {

    private final RecipeIngredientRepository recipeIngredientRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;

    /** 메뉴별 레시피 목록 */
    @Transactional(readOnly = true)
    public List<RecipeIngredientResponse> listByMenu(Long menuId) {
        Objects.requireNonNull(menuId, "MENU_ID_MUST_NOT_BE_NULL");

        // 메뉴 존재 검증
        menuItemRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

        return recipeIngredientRepository.findByMenuItemMenuId(menuId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** 레시피 등록 */
    @Transactional
    public RecipeIngredientResponse createRecipe(RecipeIngredientRequest req) {
        if (req.getConsumptionQty() == null ||
            req.getConsumptionQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_CONSUMPTION_QTY");
        }

        MenuItem menu = menuItemRepository.findById(req.getMenuId())
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

        Inventory inv = inventoryRepository.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));

        // 동일 매장 데이터인지 확인
        if (!menu.getStore().getStoreId().equals(inv.getStore().getStoreId())) {
            throw new IllegalArgumentException("STORE_MISMATCH_BETWEEN_MENU_AND_INVENTORY");
        }

        // 중복 방지
        if (recipeIngredientRepository
                .existsByMenuItemMenuIdAndInventoryItemId(req.getMenuId(), req.getItemId())) {
            throw new DuplicateKeyException("INGREDIENT_ALREADY_EXISTS_FOR_MENU");
        }

        RecipeIngredient entity = RecipeIngredient.builder()
                .menuItem(menu)
                .inventory(inv)
                .consumptionQty(req.getConsumptionQty())
                .build();

        RecipeIngredient saved = recipeIngredientRepository.save(entity);
        return toDTO(saved);
    }

    /** 레시피 수정(소모 수량만) */
    @Transactional
    public RecipeIngredientResponse updateRecipe(Long recipeId, RecipeIngredientUpdateRequest req) {
        Objects.requireNonNull(recipeId, "RECIPE_ID_MUST_NOT_BE_NULL");

        RecipeIngredient entity = recipeIngredientRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("RECIPE_INGREDIENT_NOT_FOUND"));

        if (req.getConsumptionQty() == null ||
            req.getConsumptionQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_CONSUMPTION_QTY");
        }

        entity.setConsumptionQty(req.getConsumptionQty()); // 변경감지로 업데이트
        return toDTO(entity);
    }

    /** 레시피 삭제 */
    @Transactional
    public void deleteRecipe(Long recipeId) {
        Objects.requireNonNull(recipeId, "RECIPE_ID_MUST_NOT_BE_NULL");

        RecipeIngredient entity = recipeIngredientRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("RECIPE_INGREDIENT_NOT_FOUND"));

        recipeIngredientRepository.delete(entity);
    }

    /** 엔티티 → DTO 변환 */
    private RecipeIngredientResponse toDTO(RecipeIngredient e) {
        return RecipeIngredientResponse.builder()
                .recipeId(e.getRecipeId())
                .menuId(e.getMenuItem().getMenuId())
                .itemId(e.getInventory().getItemId())
                .consumptionQty(e.getConsumptionQty())
                .build();
    }
}
