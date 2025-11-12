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
import com.erp.erp_back.entity.enums.ActiveStatus;
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

    /* ====== 목록 (Specification) ====== */
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

        // 재고 수량 입고(+)
        BigDecimal newQty = nz(item.getStockQty()).add(nz(req.getPurchaseQty()));
        item.setStockQty(newQty);

        return toDTO(saved);
    }

    /* ====== 단건 조회 ====== */
    @Transactional(readOnly = true)
    public PurchaseHistoryResponse getPurchase(Long purchaseId) {
        PurchaseHistory ph = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("PURCHASE_NOT_FOUND"));
        return toDTO(ph);
    }

    /* ====== 수정 (수량 Δ만 재고반영) ====== */
    @Transactional
    public PurchaseHistoryResponse updatePurchase(Long purchaseId, PurchaseHistoryUpdateRequest req) {
        PurchaseHistory ph = purchaseHistoryRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("PURCHASE_NOT_FOUND"));

        Inventory item = ph.getInventory();

        BigDecimal prevQty = nz(ph.getPurchaseQty());
        BigDecimal nextQty = nz(req.getPurchaseQty());
        BigDecimal delta   = nextQty.subtract(prevQty);

        BigDecimal newStock = nz(item.getStockQty()).add(delta);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("NEGATIVE_STOCK_NOT_ALLOWED");
        }
        item.setStockQty(newStock);

        ph.setPurchaseQty(nextQty);
        ph.setUnitPrice(nz(req.getUnitPrice()));
        ph.setPurchaseDate(req.getPurchaseDate());

        return toDTO(ph);
    }

    /* ====== DTO 매핑 ====== */
    private PurchaseHistoryResponse toDTO(PurchaseHistory ph) {
        return PurchaseHistoryResponse.builder()
                .purchaseId(ph.getPurchaseId())
                .storeId(ph.getStore().getStoreId())
                .itemId(ph.getInventory().getItemId())
                .purchaseQty(ph.getPurchaseQty())
                .unitPrice(ph.getUnitPrice())
                .purchaseDate(ph.getPurchaseDate())
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
