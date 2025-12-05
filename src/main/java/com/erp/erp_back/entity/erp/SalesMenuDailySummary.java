package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "SalesMenuDailySummary",
    indexes = {
        @Index(name = "idx_menu_summary_store_date", columnList = "store_id, summary_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"store_id", "menu_id", "summary_date"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesMenuDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_quantity")
    private Long totalQuantity;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;
}