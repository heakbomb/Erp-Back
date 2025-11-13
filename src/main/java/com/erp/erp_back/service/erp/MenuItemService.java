// src/main/java/com/erp/erp_back/service/erp/MenuItemService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final StoreRepository storeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    public Page<MenuItemResponse> getMenuPage(Long storeId, String q, ActiveStatus status, Pageable pageable) {
        Page<MenuItem> page;
        boolean hasQ = q != null && !q.isBlank();

        if (status == null) {
            page = hasQ
                    ? menuItemRepository.findByStoreStoreIdAndMenuNameContainingIgnoreCase(storeId, q.trim(), pageable)
                    : menuItemRepository.findByStoreStoreId(storeId, pageable);
        } else {
            page = hasQ
                    ? menuItemRepository.findByStoreStoreIdAndMenuNameContainingIgnoreCaseAndStatus(storeId, q.trim(), status, pageable)
                    : menuItemRepository.findByStoreStoreIdAndStatus(storeId, status, pageable);
        }

        // ✅ 각 메뉴별 최신가 기준 원가 계산 후 DTO에 set
        return page.map(this::toResponseWithLatestCost);
    }

    public MenuItemResponse getMenu(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        return toResponseWithLatestCost(menu);
    }

    @Transactional
    public MenuItemResponse createMenu(MenuItemRequest req) {
        var store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        if (menuItemRepository.existsByStoreStoreIdAndMenuName(req.getStoreId(), req.getMenuName().trim())) {
            throw new IllegalStateException("DUPLICATE_MENU_NAME");
        }

        MenuItem saved = menuItemRepository.save(
                MenuItem.builder()
                        .store(store)
                        .menuName(req.getMenuName().trim())
                        .price(req.getPrice())
                        .status(ActiveStatus.ACTIVE)
                        .build()
        );
        return toResponseWithLatestCost(saved);
    }

    @Transactional
    public MenuItemResponse updateMenu(Long storeId, Long menuId, MenuItemRequest req) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

        String newName = req.getMenuName().trim();
        if (!Objects.equals(menu.getMenuName(), newName) &&
                menuItemRepository.existsByStoreStoreIdAndMenuName(storeId, newName)) {
            throw new IllegalStateException("DUPLICATE_MENU_NAME");
        }

        menu.setMenuName(newName);
        menu.setPrice(req.getPrice());
        return toResponseWithLatestCost(menu);
    }

    @Transactional
    public void deactivate(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        menu.setStatus(ActiveStatus.INACTIVE);
    }

    @Transactional
    public void reactivate(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        menu.setStatus(ActiveStatus.ACTIVE);
    }

    /* ===== Helper ===== */

    private MenuItemResponse toResponseWithLatestCost(MenuItem m) {
        BigDecimal latestCost = computeMenuCostByLatest(m.getMenuId());
        return MenuItemResponse.builder()
                .menuId(m.getMenuId())
                .storeId(m.getStore().getStoreId())
                .menuName(m.getMenuName())
                .price(m.getPrice())
                .status(m.getStatus())
                // ✅ 백엔드에서 계산해 넣어줌
                .calculatedCost(latestCost)
                .build();
    }

    // Σ(소모량 × 재고.lastUnitCost). 비활성 재고는 제외(원하면 포함으로 바꿔도 됨)
    private BigDecimal computeMenuCostByLatest(Long menuId) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByMenuItemMenuId(menuId);
        BigDecimal sum = BigDecimal.ZERO;

        for (RecipeIngredient ri : ingredients) {
            Inventory inv = ri.getInventory();
            if (inv == null) continue;
            if (inv.getStatus() == ActiveStatus.INACTIVE) continue; // 비활성 재고 무시(정책)

            BigDecimal qty = nz(ri.getConsumptionQty());
            BigDecimal last = nz(inv.getLastUnitCost());
            sum = sum.add(qty.multiply(last));
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
