package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.InventorySnapshot;
import com.erp.erp_back.entity.erp.InventorySnapshotId; // ID 클래스 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

// [변경] PK 타입이 Long -> InventorySnapshotId
@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, InventorySnapshotId> {

    boolean existsBySnapshotDate(LocalDate snapshotDate);

    @Modifying
    @Query(value = """
        INSERT INTO inventory_snapshot (store_id, item_id, stock_qty, snapshot_date, created_at)
        SELECT 
            i.store_id, 
            i.item_id, 
            i.stock_qty, 
            :snapshotDate, 
            NOW()
        FROM inventory i
        WHERE i.status = 'ACTIVE'
        """, nativeQuery = true)
    void createDailySnapshot(@Param("snapshotDate") LocalDate snapshotDate);
}