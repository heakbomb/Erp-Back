package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SalesTransaction")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey; // 중복 결제 방지 키

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    // [변경] 메뉴별 금액이 아닌, 주문 전체 총액
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDiscount;

    // [신규] 결제 상태 (PAID, CANCELLED 등)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PAID";

    // [신규] 결제 수단 (CARD, CASH 등)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private String paymentMethod = "CARD";

    // [신규] 양방향 관계 설정 (영수증 하나에 여러 상세 품목)
    @OneToMany(mappedBy = "salesTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SalesLineItem> lineItems = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addLineItem(SalesLineItem item) {
        this.lineItems.add(item);
        item.setSalesTransaction(this);
    }
}