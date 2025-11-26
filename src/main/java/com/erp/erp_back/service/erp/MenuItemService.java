package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.dto.erp.MenuStatsResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.mapper.MenuItemMapper;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import static com.erp.erp_back.util.BigDecimalUtils.nz;

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
                    ? menuItemRepository.findByStoreStoreIdAndMenuNameContainingIgnoreCaseAndStatus(
                            storeId,
                            q.trim(),
                            status,
                            pageable
                    )
                    : menuItemRepository.findByStoreStoreIdAndStatus(storeId, status, pageable);
        }

        return page.map(this::toDTO);
    }

    public MenuItemResponse getMenu(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));
        return toDTO(menu);
    }

    @Transactional
    public MenuItemResponse createMenu(MenuItemRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        // ✅ [Trim 처리] Mapper에게 넘기기 전에 공백 제거
        if (req.getMenuName() != null) {
            req.setMenuName(req.getMenuName().trim());
        }

        // 중복 검사
        if (menuItemRepository.existsByStoreStoreIdAndMenuName(req.getStoreId(), req.getMenuName())) {
            throw new IllegalStateException(ErrorCodes.DUPLICATE_MENU_NAME);
        }

        MenuItem menuItem = menuItemMapper.toEntity(req, store);
        MenuItem saved = menuItemRepository.save(menuItem);

        return toDTO(saved);
    }

    @Transactional
    public MenuItemResponse updateMenu(Long storeId, Long menuId, MenuItemRequest req) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));

        // ✅ [Trim 처리]
        if (req.getMenuName() != null) {
            req.setMenuName(req.getMenuName().trim());
        }

        // 이름 변경 시 중복 검사
        String newName = req.getMenuName();
        if (newName != null
                && !Objects.equals(menu.getMenuName(), newName)
                && menuItemRepository.existsByStoreStoreIdAndMenuName(storeId, newName)) {
            throw new IllegalStateException(ErrorCodes.DUPLICATE_MENU_NAME);
        }

        menuItemMapper.updateFromDto(req, menu);

        return toDTO(menu);
    }

    @Transactional
    public void deactivate(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));
        menu.setStatus(ActiveStatus.INACTIVE);
    }

    @Transactional
    public void reactivate(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));
        menu.setStatus(ActiveStatus.ACTIVE);
    }

    @Transactional
    public void propagateCostUpdate(Long inventoryId) {
        List<RecipeIngredient> recipes = recipeIngredientRepository.findByInventoryItemId(inventoryId);

        Set<Long> menuIdsToUpdate = recipes.stream()
                .map(r -> r.getMenuItem().getMenuId())
                .collect(Collectors.toSet());

        menuIdsToUpdate.forEach(this::computeMenuCostByLatest);
    }

    /* ===== Helper ===== */

    private MenuItemResponse toDTO(MenuItem menu) {
        // 원가는 DB에 저장된 값이 아니라, 현재 시점의 재고 단가 기준으로 계산해서 보여줌
        BigDecimal latestCost = computeMenuCostByLatest(menu.getMenuId());
        return menuItemMapper.toResponse(menu, latestCost);
    }

    private BigDecimal computeMenuCostByLatest(Long menuId) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByMenuItemMenuId(menuId);
        BigDecimal sum = BigDecimal.ZERO;

        for (RecipeIngredient ri : ingredients) {
            Inventory inv = ri.getInventory();
            if (inv == null) continue;
            if (inv.getStatus() == ActiveStatus.INACTIVE) continue; // 비활성 재고 제외

            BigDecimal qty = nz(ri.getConsumptionQty());
            BigDecimal last = nz(inv.getLastUnitCost());
            sum = sum.add(qty.multiply(last));
        }
        return sum;
    }

    /** 단일 메뉴 재계산 + 저장 */
    @Transactional
    public void recalcAndSave(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));
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
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

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
        long inactive = menuItemRepository.countByStoreStoreIdAndStatus(storeId, ActiveStatus.INACTIVE);
        return new MenuStatsResponse(total, inactive);
    }
}
