package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.erp.erp_back.dto.erp.InventoryAdjustRequest;
import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public InventoryResponse createInventory(InventoryRequest req){
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

      if (inventoryRepository.existsByStoreStoreIdAndItemName(req.getStoreId(), req.getItemName())){
            throw new DuplicateKeyException("이미 존재하는 품목명입니다.");
        }

        Inventory inventory = Inventory.builder()
                    .store(store)
                    .itemName(req.getItemName().trim())
                    .itemType(req.getItemType().trim())
                    .stockType(req.getStockType().trim())
                    .stockQty(req.getStockQty().setScale(3))
                    .safetyQty(req.getSafetyQty().setScale(3))
                    .build();

        Inventory saved = inventoryRepository.save(inventory);
        return toDTO(saved);
    }

    @Transactional(readOnly=true)
    public Page<InventoryResponse> list(Long storeId, String q, Pageable pageable) {
        String keyword = (q == null) ? "" : q.trim();
        return inventoryRepository
                .findByStoreStoreIdAndItemNameContaining(storeId, keyword, pageable)
                .map(this::toDTO);
    }

    public InventoryResponse findInventoryById(Long itemId){
        Inventory inventory = inventoryRepository.findById(itemId)
                    .orElseThrow(()->new EntityNotFoundException("ITEM_NOT_FOUND"));
        
        return toDTO(inventory);
    }

     @Transactional
    public InventoryResponse adjustQty(Long itemId, InventoryAdjustRequest req) {
        Inventory e = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("ITEM_NOT_FOUND"));

        BigDecimal newQty = e.getStockQty().add(req.getDeltaQty());
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("NEGATIVE_STOCK_NOT_ALLOWED");
        }
        e.setStockQty(newQty.setScale(3));
        return toDTO(e);
    }
    
     @Transactional
    public InventoryResponse updateInventory(Long itemId, InventoryRequest req /* Long ownerId 생략 */) {
        Inventory inventory = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("ITEM_NOT_FOUND"));

        // 같은 매장 내에서만 수정 허용
        if (!inventory.getStore().getStoreId().equals(req.getStoreId())) {
            throw new IllegalArgumentException("STORE_ID_MISMATCH");
        }

        // 자기 자신 제외 중복 체크
        if (inventoryRepository.existsByStoreStoreIdAndItemNameAndItemIdNot(
                req.getStoreId(), req.getItemName(), itemId)) {
            throw new DuplicateKeyException("이미 존재하는 품목명입니다.");
        }

        inventory.setItemName(req.getItemName().trim());
        inventory.setItemType(req.getItemType().trim());
        inventory.setStockType(req.getStockType().trim());
        inventory.setStockQty(req.getStockQty().setScale(3));
        inventory.setSafetyQty(req.getSafetyQty().setScale(3));

        return toDTO(inventory);
    }
    @Transactional
    public void deleteInventory(Long itemId) {
        if (!inventoryRepository.existsById(itemId)) {
            throw new EntityNotFoundException("ITEM_NOT_FOUND");
        }
        inventoryRepository.deleteById(itemId);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        inventoryRepository.deleteAllById(ids);
    }


    private InventoryResponse toDTO(Inventory inventory) {
        return InventoryResponse.builder()
                .itemId(inventory.getItemId())
                .storeId(inventory.getStore().getStoreId())
                .itemName(inventory.getItemName())
                .itemType(inventory.getItemType())
                .stockType(inventory.getStockType())
                .stockQty(inventory.getStockQty())
                .safetyQty(inventory.getSafetyQty())
                .build();
    }

    
}
