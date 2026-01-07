package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_snapshot", 
    indexes = {
        @Index(name = "ix_snapshot_store_date", columnList = "store_id, snapshot_date")
    },
    // [핵심] 복합키 대신 유니크 제약조건으로 데이터 중복 방지
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_snapshot_store_item_date", 
            columnNames = {"store_id", "item_id", "snapshot_date"}
        )
    }
)
// [수정 1] @IdClass 제거 (IDENTITY 전략과 충돌 방지)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id // [수정 2] snapshotId만 유일한 PK로 설정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // 외래키 제약조건 생성 방지 (기존 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Store store;

    // 외래키 제약조건 생성 방지 (기존 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Inventory inventory;

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}