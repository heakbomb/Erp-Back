package com.erp.erp_back.entity.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.erp.erp_back.entity.enums.VerificationStatus;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "phone_verify_requests") // (주의) MySQL에 생성한 테이블 이름
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerifyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 10)
    private String authCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // --- 헬퍼 메소드 ---

    public void verify() {
        this.status = VerificationStatus.VERIFIED;
    }

    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    @Builder
    public PhoneVerifyRequest(String phoneNumber, String authCode, VerificationStatus status, LocalDateTime expiresAt) {
        this.phoneNumber = phoneNumber;
        this.authCode = authCode;
        this.status = status;
        this.expiresAt = expiresAt;
    }
}
