package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.dto.erp.MenuStatsResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.mapper.MenuItemMapper;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.service.erp.component.MenuCostCalculator;
import static com.erp.erp_back.repository.specification.MenuItemSpecification.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final StoreRepository storeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final MenuItemMapper menuItemMapper;

    private final MenuCostCalculator menuCostCalculator;

    public Page<MenuItemResponse> getMenuPage(Long storeId, String q, ActiveStatus status, Pageable pageable) {
        
        // 1. 기본 조건: storeId (Specification.where 없이 바로 대입)
        Specification<MenuItem> spec = byStoreId(storeId);

        // 2. 검색어(q)가 있으면 AND 조건 조립
        if (q != null && !q.isBlank()) {
            spec = spec.and(menuNameContains(q.trim()));
        }

        // 3. 상태(status)가 있으면 AND 조건 조립
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }

        // 4. 조회 (Repository에 JpaSpecificationExecutor 상속 필수!)
        Page<MenuItem> page = menuItemRepository.findAll(spec, pageable);

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

        if (menuIdsToUpdate.isEmpty()) return;

        List<MenuItem> menus = menuItemRepository.findAllById(menuIdsToUpdate);

        for (MenuItem menu : menus) {
            BigDecimal newCost = menuCostCalculator.calculate(menu.getMenuId());
            menu.setCalculatedCost(newCost);
        }
    }

    /* ===== Helper ===== */

    private MenuItemResponse toDTO(MenuItem menu) {
        // 원가는 DB에 저장된 값이 아니라, 현재 시점의 재고 단가 기준으로 계산해서 보여줌
        BigDecimal latestCost = menuCostCalculator.calculate(menu.getMenuId());
        return menuItemMapper.toResponse(menu, latestCost);
    }

    @Transactional
    public void recalcAndSave(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND));
        BigDecimal cost = menuCostCalculator.calculate(menuId);
        menu.setCalculatedCost(cost);
    }

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

    public MenuStatsResponse getMenuStats(Long storeId) {
        long total = menuItemRepository.countByStoreStoreId(storeId);
        long inactive = menuItemRepository.countByStoreStoreIdAndStatus(storeId, ActiveStatus.INACTIVE);
        return new MenuStatsResponse(total, inactive);
    }
}
