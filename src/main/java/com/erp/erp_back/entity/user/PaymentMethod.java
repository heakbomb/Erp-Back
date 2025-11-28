package com.erp.erp_back.entity.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "PaymentMethod")
@Data
@NoArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "billing_key", nullable = false, length = 100)
    private String billingKey;

    @Column(name = "card_name", length = 50)
    private String cardName;

    @Column(name = "card_number", length = 4)
    private String cardNumber;

    // ✅ [누락된 필드 추가]
    @Column(name = "provider", length = 20)
    private String provider; // 예: "PORTONE"

    @Column(name = "is_default")
    private boolean isDefault;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}