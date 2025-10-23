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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "item_name"})
})
@Data
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

    @Column(name = "stock_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "safety_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal safetyQty;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}