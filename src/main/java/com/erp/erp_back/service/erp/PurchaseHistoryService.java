// src/main/java/com/erp/erp_back/service/erp/PurchaseHistoryService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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
import com.erp.erp_back.mapper.PurchaseHistoryMapper;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.PurchaseHistoryRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.common.ErrorCodes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import static com.erp.erp_back.util.BigDecimalUtils.nz;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) 
public class PurchaseHistoryService {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final MenuItemService menuItemService;
    private final PurchaseHistoryMapper purchaseHistoryMapper;

    /* ====== 목록 (Specification로 통합 필터) ====== */
    @Transactional(readOnly = true)
    public Page<PurchaseHistoryResponse> listPurchase(
            Long storeId, Long itemId, LocalDate from, LocalDate to, Pageable pageable) {

        Objects.requireNonNull(storeId, ErrorCodes.STORE_ID_MUST_NOT_BE_NULL);

        Specification<PurchaseHistory> spec = Specification.allOf(
                byStoreId(storeId),
                itemId != null ? byItemId(itemId) : null,
                from   != null ? dateGte(from)   : null,
                to     != null ? dateLte(to)     : null
        );

        Page<PurchaseHistory> page = purchaseHistoryRepository.findAll(spec, pageable);
        return page.map(purchaseHistoryMapper::toResponse);
    }

    private static Specification<PurchaseHistory> byStoreId(Long storeId) {
        return (root, query, cb) -> cb.equal(root.get("store").get("storeId"), storeId);
    }
    private static Specification<PurchaseHistory> byItemId(Long itemId) {
        return (root, query, cb) -> cb.equal(root.get("inventory").get("itemId"), itemId);
    }
    private static Specification<PurchaseHistory> dateGte(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("purchaseDate"), from);
    }
    private static Specification<PurchaseHistory> dateLte(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("purchaseDate"), to);
    }

    /* ====== 생성 ====== */
    @Transactional
    public PurchaseHistoryResponse createPurchase(PurchaseHistoryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        Inventory item = inventoryRepository.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_ITEM_NOT_FOUND));

        if (!Objects.equals(item.getStore().getStoreId(), store.getStoreId())) {
            throw new IllegalArgumentException(ErrorCodes.ITEM_NOT_BELONG_TO_STORE);
        }

        // 1) 매입 저장
        PurchaseHistory purchase = purchaseHistoryMapper.toEntity(req, store, item);
        PurchaseHistory saved = purchaseHistoryRepository.save(purchase);

        // 2) 재고 수량 증가만 반영
        BigDecimal prevQty  = nz(item.getStockQty());
        BigDecimal addQty   = nz(req.getPurchaseQty());
        item.setStockQty(prevQty.add(addQty));

        // 3) 최신단가만 재산출
        recomputeLatestCostFromHistory(item.getItemId());
        
        // ⭐ [추가] 원가 변경을 메뉴 서비스에 전파
        menuItemService.propagateCostUpdate(item.getItemId());

        return purchaseHistoryMapper.toResponse(saved);
    }

    /* ====== 단건 조회 ====== */
    @Transactional(readOnly = true)
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

        Inventory item = purchase.getInventory();

        // 1) 재고 수량 보정 (Δ)
        BigDecimal prevQty = nz(purchase.getPurchaseQty());
        BigDecimal nextQty = nz(req.getPurchaseQty());
        BigDecimal delta   = nextQty.subtract(prevQty);

        BigDecimal newStock = nz(item.getStockQty()).add(delta);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(ErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED);
        }
        item.setStockQty(newStock);

        // 2) 매입 레코드 갱신
        purchase.setPurchaseQty(nextQty);
        purchase.setUnitPrice(nz(req.getUnitPrice()));
        purchase.setPurchaseDate(req.getPurchaseDate());

        // 3) 최신단가만 재산출
        recomputeLatestCostFromHistory(item.getItemId());

        // ⭐ [추가] 원가 변경을 메뉴 서비스에 전파
        menuItemService.propagateCostUpdate(item.getItemId());

        return purchaseHistoryMapper.toResponse(purchase);
    }

    /* ====== 내부: 최신단가만 재계산 ====== */
    private void recomputeLatestCostFromHistory(Long itemId) {
        Inventory inventory = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.INVENTORY_NOT_FOUND));

        List<PurchaseHistory> all = purchaseHistoryRepository.findByInventoryItemId(itemId);
        if (all.isEmpty()) {
            // 매입기록이 없으면 최신단가 0
            inventory.setLastUnitCost(BigDecimal.ZERO);
            return;
        }

        // 최신단가 = '구매일' 최신(동일일자는 purchaseId 큰 것) 레코드의 unitPrice
        PurchaseHistory latest = all.stream()
                .max(Comparator
                        .<PurchaseHistory, LocalDate>comparing(p -> p.getPurchaseDate() == null ? LocalDate.MIN : p.getPurchaseDate())
                        .thenComparing(PurchaseHistory::getPurchaseId))
                .orElse(all.get(0));

        inventory.setLastUnitCost(nz(latest.getUnitPrice()));
    }

}