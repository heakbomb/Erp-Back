package com.erp.erp_back.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "item_name"})
})
@Getter // @Data 대신 사용
@Setter // @Data 대신 사용
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    // --- [누락된 필드 추가 1] ---
    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType; // 

    // --- [누락된 필드 추가 2] ---
    @Column(name = "stock_type", nullable = false, length = 20)
    private String stockType; // 

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty; // 

    @Column(name = "safety_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal safetyQty; // 

    @Column(name = "expiry_date")
    private LocalDate expiryDate; // 
}