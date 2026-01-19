package com.erp.erp_back.repository.auth;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.auth.EmailVerification;
import com.erp.erp_back.entity.enums.VerificationStatus;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    long countByEmailAndStatus(String email, VerificationStatus status);
    void deleteAllByExpiresAtBefore(LocalDateTime now);
}