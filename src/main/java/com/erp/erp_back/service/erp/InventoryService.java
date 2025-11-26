// src/main/java/com/erp/erp_back/service/erp/InventoryService.java
package com.erp.erp_back.service.erp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

import com.erp.erp_back.common.ErrorCodes;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import static com.erp.erp_back.repository.specification.InventorySpecification.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse createInventory(InventoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        Inventory inv = inventoryMapper.toEntity(req, store);

        Inventory saved = inventoryRepository.save(inv);
        return inventoryMapper.toResponse(saved);
    }

    public InventoryResponse getInventory(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));
        return inventoryMapper.toResponse(inv);
    }

    public Page<InventoryResponse> getInventoryPage(Long storeId, String q, ActiveStatus status, Pageable pageable) {
        Specification<Inventory> spec = byStoreId(storeId);

        // 2. 동적 조건: 검색어(q)가 있으면 AND 조건 추가 (AND item_name LIKE %?%)
        if (q != null && !q.isBlank()) {
            spec = spec.and(itemNameContains(q.trim()));
        }

        // 3. 동적 조건: 상태(status)가 있으면 AND 조건 추가 (AND status = ?)
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }

        // 4. 실행: 완성된 명세(Spec)로 조회
        Page<Inventory> page = inventoryRepository.findAll(spec, pageable);

        return page.map(inventoryMapper::toResponse);
    }

    @Transactional
    public InventoryResponse updateInventory(Long storeId, Long itemId, InventoryRequest req) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));

        inventoryMapper.updateFromDto(req, inv);

        return inventoryMapper.toResponse(inv);
    }

    @Transactional
    public void deactivate(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));
        inv.setStatus(ActiveStatus.INACTIVE);
    }

    @Transactional
    public void reactivate(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));
        inv.setStatus(ActiveStatus.ACTIVE);
    }
}