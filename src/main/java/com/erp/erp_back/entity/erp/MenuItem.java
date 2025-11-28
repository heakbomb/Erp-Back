// src/main/java/com/erp/erp_back/entity/erp/MenuItem.java
package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.util.Objects;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "MenuItem",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_menuitem_store_name", columnNames = {"store_id", "menu_name"})
    },
    indexes = {
        @Index(name = "ix_menuitem_store", columnList = "store_id"),
        @Index(name = "ix_menuitem_name", columnList = "menu_name")
    }
)
@Getter @Setter
@Builder
@AllArgsConstructor @NoArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    // 금액 컬럼은 넉넉하게 precision 늘려두는 걸 권장
    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // 산출원가는 계산 전까지 0으로 유지
    @Column(name = "calculated_cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal calculatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ActiveStatus status = ActiveStatus.ACTIVE;

    // ✔ 더블 세이프가드: 빌더/세터/직접 매핑 등으로 null 들어와도 INSERT 직전에 기본값 보장
    @PrePersist
    void prePersist() {
        if (price == null) price = BigDecimal.ZERO;
        if (calculatedCost == null) calculatedCost = BigDecimal.ZERO;
        if (status == null) status = ActiveStatus.ACTIVE;
    }

    // (선택) UPDATE 때도 혹시 모를 null 방지
    @PreUpdate
    void preUpdate() {
        if (calculatedCost == null) calculatedCost = BigDecimal.ZERO;
        if (price == null) price = BigDecimal.ZERO;
        if (status == null) status = ActiveStatus.ACTIVE;
    }

    public void validateOwner(Store targetStore) {
        if (!Objects.equals(this.store.getStoreId(), targetStore.getStoreId())) {
            throw new IllegalArgumentException(ErrorCodes.MENU_STORE_MISMATCH);
        }
    }
}
