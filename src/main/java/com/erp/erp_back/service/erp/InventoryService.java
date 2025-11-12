// src/main/java/com/erp/erp_back/service/erp/InventoryService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;

    /* ========= 생성 ========= */
    @Transactional
    public InventoryResponse createInventory(InventoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        Inventory inv = Inventory.builder()
                .store(store)
                .itemName(req.getItemName().trim())
                .itemType(req.getItemType().trim())
                .stockType(req.getStockType().trim())
                .stockQty(nonNull(req.getStockQty()))
                .safetyQty(nonNull(req.getSafetyQty()))
                .status(req.getStatus() != null ? req.getStatus() : ActiveStatus.ACTIVE)
                .build();

        Inventory saved = inventoryRepository.save(inv);
        return toResponse(saved);
    }

    /* ========= 단건 조회 ========= */
    public InventoryResponse getInventory(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));
        return toResponse(inv);
    }

    /* ========= 목록(검색/상태/페이징) ========= */
    public Page<InventoryResponse> getInventoryPage(Long storeId, String q, ActiveStatus status, Pageable pageable) {
        Page<Inventory> page;
        boolean hasQ = q != null && !q.isBlank();

        if (status == null) {
            page = hasQ
                    ? inventoryRepository.findByStoreStoreIdAndItemNameContainingIgnoreCase(storeId, q.trim(), pageable)
                    : inventoryRepository.findByStoreStoreId(storeId, pageable);
        } else {
            page = hasQ
                    ? inventoryRepository.findByStoreStoreIdAndItemNameContainingIgnoreCaseAndStatus(storeId, q.trim(), status, pageable)
                    : inventoryRepository.findByStoreStoreIdAndStatus(storeId, status, pageable);
        }

        return page.map(this::toResponse);
    }

    /* ========= 수정 ========= */
    @Transactional
    public InventoryResponse updateInventory(Long storeId, Long itemId, InventoryRequest req) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));

        inv.setItemName(req.getItemName().trim());
        inv.setItemType(req.getItemType().trim());
        inv.setStockType(req.getStockType().trim());
        inv.setStockQty(nonNull(req.getStockQty()));
        inv.setSafetyQty(nonNull(req.getSafetyQty()));

        if (req.getStatus() != null) {
            inv.setStatus(req.getStatus());
        }

        return toResponse(inv);
    }

    /* ========= 활성/비활성 전환 ========= */
    @Transactional
    public void deactivate(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));
        inv.setStatus(ActiveStatus.INACTIVE);
    }

    @Transactional
    public void reactivate(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));
        inv.setStatus(ActiveStatus.ACTIVE);
    }

    /* ========= 헬퍼 ========= */
    private BigDecimal nonNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private InventoryResponse toResponse(Inventory inv) {
        return InventoryResponse.builder()
                .itemId(inv.getItemId())
                .storeId(inv.getStore().getStoreId())
                .itemName(inv.getItemName())
                .itemType(inv.getItemType())
                .stockType(inv.getStockType())
                .stockQty(nonNull(inv.getStockQty()))
                .safetyQty(nonNull(inv.getSafetyQty()))
                .status(inv.getStatus())
                .build();
    }
}
