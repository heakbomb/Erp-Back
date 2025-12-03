package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*; // 여기에 ConstraintMode가 포함되어 있습니다.
import lombok.*;

@Entity
@Table(
    name = "inventory_snapshot", 
    indexes = {
        @Index(name = "ix_snapshot_store_date", columnList = "store_id, snapshot_date")
    }
)
@IdClass(InventorySnapshotId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Id
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // [수정 포인트 1] 외래키 제약조건 생성 방지 (ConstraintMode.NO_CONSTRAINT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Store store;

    // [수정 포인트 2] 외래키 제약조건 생성 방지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Inventory inventory;

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}