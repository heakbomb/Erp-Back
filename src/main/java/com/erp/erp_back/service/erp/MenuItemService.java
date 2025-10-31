package com.erp.erp_back.service.erp;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public MenuItemResponse createMenu(MenuItemRequest req /* Long loginOwnerId 생략 */) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        if (menuItemRepository.existsByStoreStoreIdAndMenuName(req.getStoreId(), req.getMenuName())) {
            throw new DuplicateKeyException("이미 존재하는 메뉴명입니다.");
        }
        MenuItem menu = MenuItem.builder()
                .store(store)
                .menuName(req.getMenuName().trim())
                .price(req.getPrice())
                .calculatedCost(new BigDecimal("0.00"))
                .build();

        MenuItem saved = menuItemRepository.save(menu);
        return toDTO(saved);

    }

    @Transactional(readOnly = true)
    public MenuItemResponse findMenuById(Long menuId) {
        MenuItem m = menuItemRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴가 존재하지 않습니다."));
        return toDTO(m);
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> list(Long storeId, String q, Pageable pageable) {
        String keyword = (q == null) ? "" : q.trim();
        return menuItemRepository
                .findByStoreStoreIdAndMenuNameContaining(storeId, keyword, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public MenuItemResponse updateMenu(Long menuId, MenuItemRequest req /* ownerId는 지금 생략 */) {

        MenuItem menu = menuItemRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

        // storeId 변경은 허용 안 함(정책)
        if (!menu.getStore().getStoreId().equals(req.getStoreId())) {
            throw new IllegalArgumentException("STORE_ID_MISMATCH"); // 같은 매장 내 수정만 허용
        }

        // 동일 매장 내 이름 중복 방지(자기 자신 제외)
        if (menuItemRepository.existsByStoreStoreIdAndMenuNameAndMenuIdNot(
                req.getStoreId(), req.getMenuName(), menuId)) {
            throw new org.springframework.dao.DuplicateKeyException("이미 존재하는 메뉴명입니다.");
        }

        menu.setMenuName(req.getMenuName().trim());
        menu.setPrice(req.getPrice());
        // 레시피 변경/원가 재계산 로직이 있다면 여기서 m.setCalculatedCost(...)

        return toDTO(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        if (!menuItemRepository.existsById(menuId)) {
            throw new EntityNotFoundException("메뉴가 존재하지 않습니다.");
        }
        menuItemRepository.deleteById(menuId);
    }

    @Transactional
    public MenuItemResponse updateCalculatedCost(Long menuId, BigDecimal newCost) {
        MenuItem m = menuItemRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));
        m.setCalculatedCost(newCost.setScale(2)); // DB 스케일(2) 정렬
        return toDTO(m);
    }

    private MenuItemResponse toDTO(MenuItem m) {
        return MenuItemResponse.builder()
                .menuId(m.getMenuId())
                .storeId(m.getStore().getStoreId()) // 연관 엔티티에서 PK만 꺼냄
                .menuName(m.getMenuName())
                .price(m.getPrice())
                .calculatedCost(m.getCalculatedCost())
                .build();
    }
}
