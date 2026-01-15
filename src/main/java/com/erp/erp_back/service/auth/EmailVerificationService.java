package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.auth.EmailVerification;
import com.erp.erp_back.entity.enums.VerificationStatus;
import com.erp.erp_back.infra.auth.VerificationCodeManager;
import com.erp.erp_back.infra.mail.EmailSender;
import com.erp.erp_back.repository.auth.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TTL_MINUTES = 3;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SEC = 30;

    private final EmailVerificationRepository repo;
    private final VerificationCodeManager codeManager;
    private final EmailSender mailSender;

    @Transactional
    public String send(String email) {
        String normalized = email.trim().toLowerCase();

        String code = codeManager.generate6Digits();
        String hash = codeManager.hash(code);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(TTL_MINUTES);

        EmailVerification v = EmailVerification.create(normalized, hash, expiresAt);
        repo.save(v);

        mailSender.sendVerificationCode(normalized, code);

        return v.getVerificationId();
    }

    @Transactional
    public void resend(String verificationId) {
        EmailVerification v = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("verification not found"));

        LocalDateTime now = LocalDateTime.now();

        // 쿨다운
        if (v.getLastSentAt() != null) {
            long diffSec = java.time.Duration.between(v.getLastSentAt(), now).getSeconds();
            if (diffSec < RESEND_COOLDOWN_SEC) {
                throw new IllegalStateException("too fast resend");
            }
        }

        // 이미 검증 완료면 재전송 불필요
        if (v.getStatus() == VerificationStatus.VERIFIED) {
            return;
        }

        String code = codeManager.generate6Digits();
        String hash = codeManager.hash(code);
        LocalDateTime expiresAt = now.plusMinutes(TTL_MINUTES);

        v.markResent(hash, now, expiresAt);
        mailSender.sendVerificationCode(v.getEmail(), code);
    }

    @Transactional
    public boolean confirm(String verificationId, String code) {
        EmailVerification v = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("verification not found"));

        LocalDateTime now = LocalDateTime.now();

        if (v.getStatus() == VerificationStatus.VERIFIED)
            return true;

        if (v.isExpired(now)) {
            v.markExpired();
            return false;
        }

        if (v.getAttemptCount() >= MAX_ATTEMPTS) {
            // 시도 초과 시 만료 처리(정책)
            v.markExpired();
            return false;
        }

        v.increaseAttempt();

        String hash = codeManager.hash(code.trim());
        if (!hash.equals(v.getCodeHash())) {
            return false;
        }

        v.markVerified(now);
        return true;
    }

    @Transactional(readOnly = true)
    public VerificationStatus status(String verificationId) {
        EmailVerification v = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("verification not found"));
        return v.getStatus();
    }

    @Transactional
    public void cleanupExpired() {
        repo.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public String getEmail(String verificationId) {
        EmailVerification v = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("verification not found"));
        return v.getEmail();
    }

    @Transactional
    public void consume(String verificationId) {
        EmailVerification v = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("verification not found"));

        // VERIFIED 상태에서만 소모 허용
        if (v.getStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("verification not verified");
        }

        // 재사용 방지 정책: 검증 레코드 삭제 (가장 간단/안전)
        repo.delete(v);
    }
}