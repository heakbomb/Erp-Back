// src/main/java/com/erp/erp_back/entity/erp/MenuItem.java
package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.util.Objects;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "MenuItem",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_menuitem_store_name", columnNames = {"store_id", "menu_name"})
    },
    indexes = {
        @Index(name = "ix_menuitem_store", columnList = "store_id"),
        @Index(name = "ix_menuitem_name", columnList = "menu_name"),
        // ✅ [추가] 조회 최적화용
        @Index(name = "ix_menuitem_category_name", columnList = "category_name"),
        @Index(name = "ix_menuitem_sub_category_name", columnList = "sub_category_name")
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

    @Column(name = "menu_name", nullable = false, length = 20)
    private String menuName;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "calculated_cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal calculatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ActiveStatus status = ActiveStatus.ACTIVE;

    // =========================================================
    // ✅ [추가] 중분류/소분류 (코드에서 정해진 값만 저장)
    // =========================================================
    @Column(name = "category_name", nullable = false, length = 30)
    private String categoryName;

    @Column(name = "sub_category_name", nullable = false, length = 30)
    private String subCategoryName;

    @PrePersist
    void prePersist() {
        if (price == null) price = BigDecimal.ZERO;
        if (calculatedCost == null) calculatedCost = BigDecimal.ZERO;
        if (status == null) status = ActiveStatus.ACTIVE;
    }

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