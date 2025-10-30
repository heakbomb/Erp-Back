package com.erp.erp_back.entity.auth;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "employee_assignment", // ✅ 실제 테이블명과 통일
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "store_id"})
    }
)
@Data
@NoArgsConstructor
public class EmployeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // ✅ 기본값 (JPA insert 시 적용)

    // ✅ 기본값 보장 (DB default 없이도 안전)
    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "PENDING";
        }
    }
}