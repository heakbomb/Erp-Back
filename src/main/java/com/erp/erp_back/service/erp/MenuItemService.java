// src/main/java/com/erp/erp_back/service/erp/MenuItemService.java
package com.erp.erp_back.service.erp;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /* ========= 생성 ========= */
    @Transactional
    public MenuItemResponse createMenu(MenuItemRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        String name = req.getMenuName() == null ? "" : req.getMenuName().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("MENU_NAME_REQUIRED");
        }

        if (menuItemRepository.existsByStoreStoreIdAndMenuName(req.getStoreId(), name)) {
            throw new DuplicateKeyException("DUPLICATE_MENU_NAME");
        }

        MenuItem menu = MenuItem.builder()
                .store(store)
                .menuName(name)
                .price(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice())
                .calculatedCost(BigDecimal.ZERO) // not null 보장
                .status(req.getStatus() != null ? req.getStatus() : ActiveStatus.ACTIVE)
                .build();

        MenuItem saved = menuItemRepository.save(menu);
        return toResponse(saved);
    }

    /* ========= 단건 조회 ========= */
    public MenuItemResponse getMenu(Long storeId, Long menuId) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        return toResponse(menu);
    }

    /* ========= 목록(검색/상태/페이징) ========= */
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
        return page.map(this::toResponse);
    }

    /* ========= 수정 ========= */
    @Transactional
    public MenuItemResponse updateMenu(Long storeId, Long menuId, MenuItemRequest req) {
        MenuItem menu = menuItemRepository.findByMenuIdAndStoreStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

        if (req.getMenuName() != null) {
            String newName = req.getMenuName().trim();
            if (!menu.getMenuName().equalsIgnoreCase(newName)
                    && menuItemRepository.existsByStoreStoreIdAndMenuName(storeId, newName)) {
                throw new DuplicateKeyException("DUPLICATE_MENU_NAME");
            }
            menu.setMenuName(newName);
        }
        if (req.getPrice() != null) {
            menu.setPrice(req.getPrice());
        }
        if (req.getStatus() != null) {
            menu.setStatus(req.getStatus());
        }
        // 원가(calculatedCost)는 레시피/인벤토리 변경 훅에서 갱신

        return toResponse(menu);
    }

    /* ========= 활성/비활성 전환 ========= */
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


    /* ========= 헬퍼 ========= */
    private MenuItemResponse toResponse(MenuItem m) {
        BigDecimal cost = m.getCalculatedCost() == null ? BigDecimal.ZERO : m.getCalculatedCost();
        return MenuItemResponse.builder()
                .menuId(m.getMenuId())
                .storeId(m.getStore().getStoreId())
                .menuName(m.getMenuName())
                .price(m.getPrice())
                .calculatedCost(cost)
                .status(m.getStatus())
                .build();
    }

    private BigDecimal nonNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
