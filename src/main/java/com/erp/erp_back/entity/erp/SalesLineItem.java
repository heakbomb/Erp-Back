package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "SalesLineItem",
        indexes = {
                @Index(name = "idx_lineitem_transaction", columnList = "transaction_id"),
                @Index(name = "idx_lineitem_menu", columnList = "menu_id")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    // 어떤 영수증(주문)에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private SalesTransaction salesTransaction;

    // 어떤 메뉴를 시켰는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuItem menuItem;

    // 몇 개 시켰는지 (재고 차감의 기준)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 판매 당시 단가 (메뉴 가격이 나중에 바뀌어도 기록 유지)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 라인별 총액 (quantity * unitPrice)
    @Column(name = "line_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineAmount;
}