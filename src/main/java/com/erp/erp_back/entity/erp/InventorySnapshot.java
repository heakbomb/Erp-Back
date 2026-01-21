package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "inventory_snapshot",
    // [추가] PK 대신 유니크 제약조건으로 중복 방지 (같은 매장+상품+날짜 중복 불가)
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_snapshot_store_item_date",
            columnNames = {"store_id", "item_id", "snapshot_date"}
        )
    },
    indexes = {
        @Index(name = "ix_snapshot_store_date", columnList = "store_id, snapshot_date"),
        @Index(name = "ix_snapshot_date", columnList = "snapshot_date")
    }
)
// 🚨 삭제됨: @IdClass(InventorySnapshotId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id // ✅ 단일 PK로 설정 (Auto Increment 가능)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Inventory inventory;

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}