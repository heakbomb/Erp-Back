// src/main/java/com/erp/erp_back/service/erp/PurchaseHistoryService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.PurchaseHistoryRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryResponse;
import com.erp.erp_back.dto.erp.PurchaseHistoryUpdateRequest;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.exception.BusinessException;
import com.erp.erp_back.mapper.PurchaseHistoryMapper;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.PurchaseHistoryRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.common.ErrorCodes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import static com.erp.erp_back.util.BigDecimalUtils.nz;
import static com.erp.erp_back.repository.specification.PurchaseHistorySpecification.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseHistoryService {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final MenuItemService menuItemService;
    private final PurchaseHistoryMapper purchaseHistoryMapper;
    private final InventoryService inventoryService;
    

    /* ====== 목록 (Specification로 통합 필터) ====== */
    public Page<PurchaseHistoryResponse> listPurchase(
            Long storeId, Long itemId, LocalDate from, LocalDate to, Pageable pageable) {

        Objects.requireNonNull(storeId, ErrorCodes.STORE_ID_MUST_NOT_BE_NULL);

        Specification<PurchaseHistory> spec = byStoreId(storeId);

        if (itemId != null) {
            spec = spec.and(byItemId(itemId));
        }
        if (from != null) {
            spec = spec.and(dateGte(from));
        }
        if (to != null) {
            spec = spec.and(dateLte(to));
        }

        Page<PurchaseHistory> page = purchaseHistoryRepository.findAll(spec, pageable);
        return page.map(purchaseHistoryMapper::toResponse);
    }

    @Transactional
    public PurchaseHistoryResponse createPurchase(PurchaseHistoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        Inventory item = inventoryRepository.findByIdWithLock(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_ITEM_NOT_FOUND));

        if (!Objects.equals(item.getStore().getStoreId(), store.getStoreId())) {
            throw new IllegalArgumentException(ErrorCodes.ITEM_NOT_BELONG_TO_STORE);
        }

        item.activateIfPurchased(req.getPurchaseQty());

        PurchaseHistory purchase = purchaseHistoryMapper.toEntity(req, store, item);
        PurchaseHistory saved = purchaseHistoryRepository.save(purchase);

        item.adjustStock(nz(req.getPurchaseQty()));

        recomputeLatestCostFromHistory(item);
        menuItemService.propagateCostUpdate(item.getItemId());

        return purchaseHistoryMapper.toResponse(saved);

    }

    /* ====== 단건 조회 ====== */
    public PurchaseHistoryResponse getPurchase(Long purchaseId) {
        PurchaseHistory purchase = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.PURCHASE_NOT_FOUND));
        return purchaseHistoryMapper.toResponse(purchase);
    }

    /* ====== 수정 ====== */
    @Transactional
    public PurchaseHistoryResponse updatePurchase(Long purchaseId, PurchaseHistoryUpdateRequest req) {
        PurchaseHistory purchase = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.PURCHASE_NOT_FOUND));

        Long itemId = purchase.getInventory().getItemId();

        Inventory item = inventoryRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_ITEM_NOT_FOUND));

        BigDecimal prevQty = nz(purchase.getPurchaseQty()); // 기존 매입량
        BigDecimal nextQty = nz(req.getPurchaseQty()); // 수정된 매입량
        BigDecimal delta = nextQty.subtract(prevQty); // 차이 (예: +2 or -5)

        item.activateIfPurchased(req.getPurchaseQty());

        try {
            item.adjustStock(delta);
        } catch (IllegalArgumentException ex) {
            if (ErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED.equals(ex.getMessage())) {
                throw new BusinessException(
                        ErrorCodes.PURCHASE_QTY_LESS_THAN_SOLD,
                        "이미 판매된 수량 때문에 이 매입 수량으로 줄이면 재고가 음수가 되어 수정할 수 없습니다.");
            }
            throw ex; // 다른 IllegalArgumentException이면 그대로 던짐
        }

        purchase.setPurchaseQty(nextQty);
        purchase.setUnitPrice(nz(req.getUnitPrice()));
        purchase.setPurchaseDate(req.getPurchaseDate());

        onPurchaseChanged(item);

        return purchaseHistoryMapper.toResponse(purchase);
    }

    @Transactional
    public void deletePurchase(Long purchaseId) {
        PurchaseHistory purchase = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.PURCHASE_NOT_FOUND));

        Long itemId = purchase.getInventory().getItemId();

        Inventory item = inventoryRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_ITEM_NOT_FOUND));

        // 재고 원복 (매입했던 수량을 다시 뺌)
        try {
            // 삭제 = 과거 매입을 없애는 거라, 당시 매입량만큼 재고에서 빼줌
            item.adjustStock(purchase.getPurchaseQty().negate());
        } catch (IllegalArgumentException ex) {
            // 재고 음수 도메인 규칙 위반이면 → 사용자 친화적인 비즈니스 예외로 포장
            if (ErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED.equals(ex.getMessage())) {
                throw new BusinessException(
                        ErrorCodes.PURCHASE_DELETE_BELOW_CONSUMED,
                        "이미 판매된 수량 때문에 이 매입 내역을 삭제하면 재고가 음수가 되어 삭제할 수 없습니다.");
            }
            throw ex; // 다른 IllegalArgumentException이면 그대로 올림
        }

        purchaseHistoryRepository.delete(purchase);

        // 원가 재계산
        onPurchaseChanged(item);

        // ✅ 고아 품목이면 Inventory 삭제
       inventoryService.deleteOrphanInventoryIfPossible(item);
    }

    /* ====== 내부: 최신단가만 재계산 ====== */
    private void recomputeLatestCostFromHistory(Inventory inventory) {

        BigDecimal latestPrice = purchaseHistoryRepository
                .findTop1ByInventoryItemIdOrderByPurchaseDateDescPurchaseIdDesc(inventory.getItemId())
                .map(PurchaseHistory::getUnitPrice)
                .orElse(BigDecimal.ZERO);

        inventory.setLastUnitCost(latestPrice);
    }

    private void onPurchaseChanged(Inventory item) {
        recomputeLatestCostFromHistory(item);
        menuItemService.propagateCostUpdate(item.getItemId());
    }

}