// src/main/java/com/erp/erp_back/entity/erp/Inventory.java
package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.store.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "Inventory",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"store_id", "item_name"})
    },
    indexes = {
        @Index(name = "ix_inventory_store", columnList = "store_id"),
        @Index(name = "ix_inventory_name", columnList = "item_name")
    }
)
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

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType; // Service에서 builder.itemType(), setItemType()

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
}
