// src/main/java/com/erp/erp_back/service/erp/MenuItemService.java
package com.erp.erp_back.service.erp;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.dto.erp.MenuStatsResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.mapper.MenuItemMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final StoreRepository storeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final MenuItemMapper menuItemMapper;

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
        return page.map(this::toDTO);
    }

    public MenuItemResponse getMenu(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        return toDTO(menu);
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
        return toDTO(saved);
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
        return toDTO(menu);
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

    // ⭐ [추가] PurchaseHistoryService가 재고 단가 변경을 알릴 때 호출
    @Transactional
    public void propagateCostUpdate(Long inventoryId) {
        // 이 재고를 사용하는 모든 레시피 목록 조회
        List<RecipeIngredient> recipes = recipeIngredientRepository.findByInventoryItemId(inventoryId);

        // 중복 없는 menuId 목록 추출
        Set<Long> menuIdsToUpdate = recipes.stream()
                .map(r -> r.getMenuItem().getMenuId())
                .collect(Collectors.toSet());

        // 각 메뉴의 원가를 다시 계산합니다. (동적 계산이므로 DB 저장 필요 없음)
        menuIdsToUpdate.forEach(this::computeMenuCostByLatest);
    }

    /* ===== Helper ===== */

    private MenuItemResponse toDTO(MenuItem menu) {
        BigDecimal latestCost = computeMenuCostByLatest(menu.getMenuId());
        return menuItemMapper.toResponse(menu, latestCost);
    }

    // Σ(소모량 × 재고.lastUnitCost). 비활성 재고는 제외(원하면 포함으로 바꿔도 됨)
    private BigDecimal computeMenuCostByLatest(Long menuId) {
    List<RecipeIngredient> ingredients =
            recipeIngredientRepository.findByMenuItemMenuId(menuId);
    BigDecimal sum = BigDecimal.ZERO;

    for (RecipeIngredient ri : ingredients) {
        Inventory inv = ri.getInventory();
        if (inv == null) continue;
        if (inv.getStatus() == ActiveStatus.INACTIVE) continue; // 비활성 재고 제외

        BigDecimal qty  = nz(ri.getConsumptionQty());
        BigDecimal last = nz(inv.getLastUnitCost());
        sum = sum.add(qty.multiply(last));
    }
    return sum;
}

    /** 단일 메뉴 재계산 + 저장 */
@Transactional
public void recalcAndSave(Long storeId, Long menuId) {
    MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
            .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
    BigDecimal cost = computeMenuCostByLatest(menuId);
    menu.setCalculatedCost(cost);
}

/** 특정 재고(itemId)를 쓰는 모든 메뉴 재계산 */
@Transactional
public void recalcByInventory(Long storeId, Long itemId) {
    List<RecipeIngredient> used = recipeIngredientRepository.findByInventoryItemId(itemId);
    for (RecipeIngredient ri : used) {
        MenuItem menu = ri.getMenuItem();
        if (menu != null && Objects.equals(menu.getStore().getStoreId(), storeId)) {
            BigDecimal cost = computeMenuCostByLatest(menu.getMenuId());
            menu.setCalculatedCost(cost);
        }
    }
}

@Transactional(readOnly = true)
public List<MenuItemResponse> listActiveMenusForPos(Long storeId) {

    storeRepository.findById(storeId)
            .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

    Page<MenuItem> page = menuItemRepository.findByStoreStoreIdAndStatus(
            storeId,
            ActiveStatus.ACTIVE,
            Pageable.unpaged()
    );

    return page.getContent().stream()
            .map(this::toDTO)  
            .toList();
}

@Transactional(readOnly = true)
public MenuStatsResponse getMenuStats(Long storeId) {
    long total = menuItemRepository.countByStoreStoreId(storeId);
    long inactive = menuItemRepository.countByStoreStoreIdAndStatus(
            storeId, ActiveStatus.INACTIVE
    );
    return new MenuStatsResponse(total, inactive);
}


    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}