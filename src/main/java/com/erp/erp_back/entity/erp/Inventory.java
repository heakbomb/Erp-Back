// src/main/java/com/erp/erp_back/entity/erp/Inventory.java
package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.enums.IngredientCategory;
import com.erp.erp_back.entity.store.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "store_id", "item_name" })
}, indexes = {
        @Index(name = "ix_inventory_store", columnList = "store_id"),
        @Index(name = "ix_inventory_name", columnList = "item_name")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId; // Service에서 getItemId() 호출

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "item_name", nullable = false, length = 20)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private IngredientCategory itemType;// Service에서 builder.itemType(), setItemType()

    @Column(name = "stock_type", nullable = false, length = 20)
    private String stockType; // Service에서 setStockType()

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty; // Service에서 get/setStockQty()

    @Column(name = "safety_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal safetyQty; // Service에서 get/setSafetyQty()

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 10)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @Builder.Default
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal lastUnitCost = BigDecimal.ZERO;

    public void adjustStock(BigDecimal delta) {
        BigDecimal current = this.stockQty != null ? this.stockQty : BigDecimal.ZERO;
        BigDecimal change = delta != null ? delta : BigDecimal.ZERO;

        BigDecimal nextStock = current.add(change);

        // 도메인 규칙: 재고는 음수가 될 수 없다.
        if (nextStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(ErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED);
        }

        // 내부 상태 변경
        this.stockQty = nextStock;
    }
}
