package com.erp.erp_back.entity.subscripition;

import java.time.LocalDate;

import com.erp.erp_back.entity.user.Owner;

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
@Table(name = "OwnerSubscription", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"owner_id", "sub_id", "start_date"})
})
@Data
@NoArgsConstructor
public class OwnerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_sub_id")
    private Long ownerSubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_id", nullable = false)
    private Subscription subscription;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // [추가] 해지 여부 (true면 다음 달 자동 결제 안 함)
    @Column(nullable = false)
    private boolean canceled = false;

    // [추가] 해지 사유
    @Column(name = "cancel_reason")
    private String cancelReason;
}