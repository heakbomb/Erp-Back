// src/main/java/com/erp/erp_back/service/erp/InventoryService.java
package com.erp.erp_back.service.erp;

import static com.erp.erp_back.repository.specification.InventorySpecification.byStoreId;
import static com.erp.erp_back.repository.specification.InventorySpecification.hasStatus;
import static com.erp.erp_back.repository.specification.InventorySpecification.itemNameContains;
import static com.erp.erp_back.util.BigDecimalUtils.nz;
import static com.erp.erp_back.repository.specification.InventorySpecification.hasItemType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.enums.IngredientCategory;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.exception.BusinessException;
import com.erp.erp_back.mapper.InventoryMapper;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.PurchaseHistoryRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse createInventory(InventoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        String name = req.getItemName().trim();

        if (inventoryRepository.existsByStoreStoreIdAndItemName(store.getStoreId(), name)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_INVENTORY_NAME, "이미 존재하는 품목명입니다.");
        }

        Inventory inv = inventoryMapper.toEntity(req, store);
        Inventory saved = inventoryRepository.save(inv);
        return inventoryMapper.toResponse(saved);
    }

    public InventoryResponse getInventory(Long storeId, Long itemId) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));
        return inventoryMapper.toResponse(inv);
    }

    public Page<InventoryResponse> getInventoryPage(Long storeId, String q, ActiveStatus status,
            IngredientCategory itemType, Pageable pageable) {
        Specification<Inventory> spec = byStoreId(storeId);

        // 2. 동적 조건: 검색어(q)가 있으면 AND 조건 추가 (AND item_name LIKE %?%)
        if (q != null && !q.isBlank()) {
            spec = spec.and(itemNameContains(q.trim()));
        }

        // 3. 동적 조건: 상태(status)가 있으면 AND 조건 추가 (AND status = ?)
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }

        // 4. 동적 조건: 품목 타입
        if (itemType != null) {
            spec = spec.and(hasItemType(itemType));
        }

        // 4. 실행: 완성된 명세(Spec)로 조회
        Page<Inventory> page = inventoryRepository.findAll(spec, pageable);

        return page.map(inventoryMapper::toResponse);
    }

    @Transactional
    public InventoryResponse updateInventory(Long storeId, Long itemId, InventoryRequest req) {
        Inventory inv = inventoryRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));

        // ✅ 이름이 null/blank가 아닐 때만 체크 (req 구조에 맞게 조정)
        String newName = req.getItemName() == null ? null : req.getItemName().trim();

        // ✅ “이름 변경”이 발생할 때만 중복 체크 (불필요한 쿼리 줄임)
        if (newName != null && !newName.isBlank() && !newName.equals(inv.getItemName())) {
            boolean duplicated = inventoryRepository.existsByStoreStoreIdAndItemNameAndItemIdNot(storeId, newName,
                    itemId);

            if (duplicated) {
                throw new BusinessException(
                        ErrorCodes.DUPLICATE_INVENTORY_NAME,
                        "이미 존재하는 품목명입니다.");
            }
        }

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

    @Transactional
    public void decreaseStock(Map<Long, BigDecimal> requirements) {
        if (requirements.isEmpty())
            return;

        List<Inventory> inventories = inventoryRepository.findAllByIdInWithLock(requirements.keySet());

        for (Inventory inv : inventories) {
            BigDecimal needed = requirements.get(inv.getItemId());
            BigDecimal after = inv.getStockQty().subtract(needed);

            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("재고 부족: " + inv.getItemName());
            }
            inv.setStockQty(after);
        }
    }

    @Transactional
    public void increaseStock(Map<Long, BigDecimal> requirements) {
        if (requirements.isEmpty())
            return;

        List<Inventory> inventories = inventoryRepository.findAllByIdInWithLock(requirements.keySet());

        for (Inventory inv : inventories) {
            BigDecimal addQty = requirements.get(inv.getItemId());
            inv.setStockQty(inv.getStockQty().add(addQty));
        }
    }

    public long countLowStockItems(Long storeId) {
        return inventoryRepository.countLowStockItems(storeId, ActiveStatus.ACTIVE);
    }

    public void deleteOrphanInventoryIfPossible(Inventory item) {
    Long itemId = item.getItemId();

    // 1) 다른 매입 내역이 남아있으면 삭제 금지
    if (purchaseHistoryRepository.existsByInventoryItemId(itemId)) return;

    // 2) 레시피에서 사용 중이면 삭제 금지
    if (recipeIngredientRepository.existsByInventoryItemId(itemId)) return;

    // 3) 재고가 0이 아니면 삭제 금지 (안전)
    BigDecimal stockQty = nz(inventoryRepository.findStockQtyByItemId(itemId));
    if (stockQty.compareTo(BigDecimal.ZERO) != 0) return;

    // ✅ 여기까지 오면 “고아 품목” → 삭제
    inventoryRepository.delete(item);
}
}