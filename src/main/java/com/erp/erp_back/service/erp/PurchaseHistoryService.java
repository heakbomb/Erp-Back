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
import com.erp.erp_back.dto.erp.PurchaseHistoryUpdateRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.PurchaseHistoryRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseHistoryService {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;

    /* ====== 목록 (Specification로 통합 필터) ====== */
    @Transactional(readOnly = true)
    public Page<PurchaseHistoryResponse> listPurchase(
            Long storeId, Long itemId, LocalDate from, LocalDate to, Pageable pageable) {

        Objects.requireNonNull(storeId, "STORE_ID_MUST_NOT_BE_NULL");

        Specification<PurchaseHistory> spec = Specification.allOf(
                byStoreId(storeId),
                itemId != null ? byItemId(itemId) : null,
                from   != null ? dateGte(from)   : null,
                to     != null ? dateLte(to)     : null
        );

        Page<PurchaseHistory> page = purchaseHistoryRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
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
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        Inventory item = inventoryRepository.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_ITEM_NOT_FOUND"));

        if (!Objects.equals(item.getStore().getStoreId(), store.getStoreId())) {
            throw new IllegalArgumentException("ITEM_NOT_BELONG_TO_STORE");
        }

        // 정책: 비활성 재고로 매입 시 자동 활성화
        if (item.getStatus() == ActiveStatus.INACTIVE) {
            item.setStatus(ActiveStatus.ACTIVE);
        
        }

        // 매입 저장
        PurchaseHistory saved = purchaseHistoryRepository.save(
                PurchaseHistory.builder()
                        .store(store)
                        .inventory(item)
                        .purchaseQty(nz(req.getPurchaseQty()))
                        .unitPrice(nz(req.getUnitPrice()))
                        .purchaseDate(req.getPurchaseDate())
                        .build()
        );

        // 2) 재고 수량 증가만 반영 (평균원가 계산 제거)
        BigDecimal prevQty  = nz(item.getStockQty());
        BigDecimal addQty   = nz(req.getPurchaseQty());
        item.setStockQty(prevQty.add(addQty));

        // 3) 최신단가만 재산출 (구매일 최신, 동일일자면 purchaseId 큰 것)
        recomputeLatestCostFromHistory(item.getItemId());

        return toDTO(saved);
    }

    /* ====== 단건 조회 ====== */
    @Transactional(readOnly = true)
    public PurchaseHistoryResponse getPurchase(Long purchaseId) {
        PurchaseHistory purchase = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("PURCHASE_NOT_FOUND"));
        return toDTO(purchase);
    }

    /* ====== 수정 ====== */
    @Transactional
    public PurchaseHistoryResponse updatePurchase(Long purchaseId, PurchaseHistoryUpdateRequest req) {
        PurchaseHistory purchase = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("PURCHASE_NOT_FOUND"));

        Inventory item = purchase.getInventory();

        // 1) 재고 수량 보정 (Δ)
        BigDecimal prevQty = nz(purchase.getPurchaseQty());
        BigDecimal nextQty = nz(req.getPurchaseQty());
        BigDecimal delta   = nextQty.subtract(prevQty);

        BigDecimal newStock = nz(item.getStockQty()).add(delta);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("NEGATIVE_STOCK_NOT_ALLOWED");
        }
        item.setStockQty(newStock);

        // 2) 매입 레코드 갱신
        purchase.setPurchaseQty(nextQty);
        purchase.setUnitPrice(nz(req.getUnitPrice()));
        purchase.setPurchaseDate(req.getPurchaseDate());

        // 3) 최신단가만 재산출
        recomputeLatestCostFromHistory(item.getItemId());

        return toDTO(purchase);
    }

    /* ====== 내부: 최신단가만 재계산 ====== */
    private void recomputeLatestCostFromHistory(Long itemId) {
        Inventory inventory = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("INVENTORY_NOT_FOUND"));

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

    /* ====== DTO 매핑 ====== */
    private PurchaseHistoryResponse toDTO(PurchaseHistory purchase) {
        return PurchaseHistoryResponse.builder()
                .purchaseId(purchase.getPurchaseId())
                .storeId(purchase.getStore().getStoreId())
                .itemId(purchase.getInventory().getItemId())
                .purchaseQty(purchase.getPurchaseQty())
                .unitPrice(purchase.getUnitPrice())
                .purchaseDate(purchase.getPurchaseDate())
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
