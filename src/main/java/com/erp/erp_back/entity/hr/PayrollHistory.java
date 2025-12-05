// src/main/java/com/erp/erp_back/entity/hr/PayrollHistory.java
package com.erp.erp_back.entity.hr;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payroll_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_id")
    private Long payrollId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // 🔥 year_month 대신 safer name: payroll_month
    @Column(name = "payroll_month", length = 7, nullable = false)
    private String payrollMonth;   // 예: "2025-12"

    @Column(name = "work_days", nullable = false)
    private long workDays;

    @Column(name = "work_minutes", nullable = false)
    private long workMinutes;

    @Column(name = "wage_type", length = 20)
    private String wageType;       // HOURLY / MONTHLY 등

    @Column(name = "base_wage")
    private Long baseWage;         // 설정된 기본급(시급 or 월급)

    @Column(name = "gross_pay", nullable = false)
    private long grossPay;         // 총 지급액(공제 전)

    @Column(name = "deductions", nullable = false)
    private long deductions;       // 공제액 합계

    @Column(name = "net_pay", nullable = false)
    private long netPay;           // 실수령액

    @Column(name = "deduction_type", length = 50)
    private String deductionType;  // FOUR_INSURANCE / TAX_3_3 / NONE

    @Column(name = "status", length = 20, nullable = false)
    private String status;         // 예정 / 지급완료 등

    @Column(name = "paid_at")
    private LocalDateTime paidAt;  // 실제 지급 완료 시간

    // 공통 시간 필드 직접 관리
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}