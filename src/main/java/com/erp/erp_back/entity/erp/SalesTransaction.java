package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SalesTransaction")
@Data
@NoArgsConstructor
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "sales_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal salesAmount;
}