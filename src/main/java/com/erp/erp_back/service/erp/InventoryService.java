// src/main/java/com/erp/erp_back/service/erp/InventoryService.java
package com.erp.erp_back.service.erp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.mapper.InventoryMapper;
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
    private final InventoryMapper inventoryMapper;

    /* ========= 생성 ========= */
    @Transactional
    public InventoryResponse createInventory(InventoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        Inventory inv = inventoryMapper.toEntity(req, store);

        Inventory saved = inventoryRepository.save(inv);
        return inventoryMapper.toResponse(saved);
    }

    /*
     * (중략: getInventory, getInventoryPage, updateInventory, deactivate, reactivate)
     */
    public InventoryResponse getInventory(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));
        return inventoryMapper.toResponse(inv);
    }

    public Page<InventoryResponse> getInventoryPage(Long storeId, String q, ActiveStatus status, Pageable pageable) {
        Page<Inventory> page;
        boolean hasQ = q != null && !q.isBlank();

        if (status == null) {
            page = hasQ
                    ? inventoryRepository.findByStoreStoreIdAndItemNameContainingIgnoreCase(storeId, q.trim(), pageable)
                    : inventoryRepository.findByStoreStoreId(storeId, pageable);
        } else {
            page = hasQ
                    ? inventoryRepository.findByStoreStoreIdAndItemNameContainingIgnoreCaseAndStatus(storeId, q.trim(),
                            status, pageable)
                    : inventoryRepository.findByStoreStoreIdAndStatus(storeId, status, pageable);
        }

        return page.map(inventoryMapper::toResponse);
    }

    @Transactional
    public InventoryResponse updateInventory(Long storeId, Long itemId, InventoryRequest req) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));

        inventoryMapper.updateFromDto(req, inv);

        return inventoryMapper.toResponse(inv);
    }

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
}