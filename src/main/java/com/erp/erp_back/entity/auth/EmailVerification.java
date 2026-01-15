package com.erp.erp_back.entity.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.erp.erp_back.entity.enums.VerificationStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "email_verification",
       indexes = {
         @Index(name = "idx_email_status", columnList = "email,status"),
         @Index(name = "idx_expires_at", columnList = "expiresAt")
       })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @Column(name = "verification_id", length = 36)
    private String verificationId;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int sendCount;

    @Column(nullable = false)
    private LocalDateTime lastSentAt;

    public static EmailVerification create(String email, String codeHash, LocalDateTime expiresAt) {
        EmailVerification v = new EmailVerification();
        v.verificationId = UUID.randomUUID().toString();
        v.email = email;
        v.codeHash = codeHash;
        v.status = VerificationStatus.PENDING;
        v.expiresAt = expiresAt;
        v.attemptCount = 0;
        v.sendCount = 1;
        v.lastSentAt = LocalDateTime.now();
        return v;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markExpired() {
        this.status = VerificationStatus.EXPIRED;
    }

    public void markVerified(LocalDateTime now) {
        this.status = VerificationStatus.VERIFIED;
        this.verifiedAt = now;
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public void markResent(String newCodeHash, LocalDateTime now, LocalDateTime newExpiresAt) {
        this.codeHash = newCodeHash;
        this.lastSentAt = now;
        this.expiresAt = newExpiresAt;
        this.sendCount++;
        this.status = VerificationStatus.PENDING;
    }
}