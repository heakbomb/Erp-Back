package com.erp.erp_back.entity.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "employee",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}),
    indexes = {
        @Index(name = "idx_employee_provider", columnList = "provider"),
        @Index(name = "idx_employee_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // ✅ 소셜에서 이메일이 없을 수 있음(특히 카카오) → nullable 권장
    @Column(name = "email", nullable = true, length = 100)
    private String email;

    // ✅ 소셜에서 전화번호는 거의 안 내려옴 → nullable 권장
    @Column(name = "phone", nullable = true, length = 20)
    private String phone;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // ex) "google", "kakao"

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}