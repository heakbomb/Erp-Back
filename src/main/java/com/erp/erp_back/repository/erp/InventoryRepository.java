package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByItemIdAndStoreStoreId(Long itemId, Long storeId);

    boolean existsByStoreStoreIdAndItemName(Long storeId, String itemName);

    boolean existsByStoreStoreIdAndItemNameAndItemIdNot(Long storeId, String itemName, Long itemId);

    // 🔒 [비관적 락] 재고 차감/증가 시 동시성 충돌 방지 + 일괄 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.itemId IN :ids")
    List<Inventory> findAllByIdInWithLock(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.itemId = :id")
    Optional<Inventory> findByIdWithLock(@Param("id") Long id);

    @Query("""
            SELECT new com.erp.erp_back.dto.erp.InventoryResponse(
                i.itemId,
                i.store.storeId,
                i.itemName,
                i.itemType,
                i.stockType,
                i.stockQty,
                i.safetyQty,
                i.status,
                i.lastUnitCost
            )
            FROM Inventory i
            WHERE i.store.storeId = :storeId
            ORDER BY i.itemName ASC
            """)
    List<InventoryResponse> findExportRowsByStoreId(@Param("storeId") Long storeId);

    @Query("""
                SELECT COUNT(i)
                FROM Inventory i
                WHERE i.store.storeId = :storeId
                  AND i.status = :status
                  AND i.stockQty < i.safetyQty
            """)
    long countLowStockItems(@Param("storeId") Long storeId,
            @Param("status") ActiveStatus status);

    @Query("select i.stockQty from Inventory i where i.itemId = :itemId")
    BigDecimal findStockQtyByItemId(@Param("itemId") Long itemId);
}
