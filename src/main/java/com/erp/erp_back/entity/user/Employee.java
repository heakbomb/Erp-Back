package com.erp.erp_back.entity.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "employee", // 리눅스 배포 시 대소문자 이슈 방지
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Getter
@Setter // 직원 정보 수정 API 대비 (원하면 update(...) 메서드로 대체 가능)
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 100) // 필요 시 unique=true
    private String email;

    // 소셜 프로필에 전화번호가 없을 수 있으면 nullable=true로 바꾸세요.
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    // 항상 소셜이므로 NOT NULL 유지
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;          // ex) "google", "kakao", "github" ...

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;        // 소셜에서 내려주는 user id

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}