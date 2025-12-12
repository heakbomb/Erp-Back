package com.erp.erp_back.entity.hr;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "payroll_run",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_run_store_month", columnNames = {"store_id", "payroll_month"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "payroll_month", length = 7, nullable = false)
    private String payrollMonth; // "2025-12"

    @Column(name = "status", length = 20, nullable = false)
    private String status; // DRAFT / FINALIZED / FAILED

    @Column(name = "source", length = 20)
    private String source; // MANUAL / AUTO

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "DRAFT";
        // 기본 0
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}