package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.auth.PasswordResetToken;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.exception.PasswordResetException; // ✅ 추가
import com.erp.erp_back.infra.mail.PasswordResetMailSender;
import com.erp.erp_back.repository.auth.PasswordResetTokenRepository;
import com.erp.erp_back.repository.auth.RefreshTokenRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final OwnerRepository ownerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailSender passwordResetMailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * 1️⃣ 비밀번호 재설정 요청 (메일 발송)
     */
    @Transactional
    public void requestReset(String email) {

        ownerRepository.findByEmail(email).ifPresent(owner -> {

            // 기존 토큰 전부 삭제 (중복 방지)
            tokenRepository.deleteAllByOwnerId(owner.getOwnerId());

            String token = UUID.randomUUID().toString().replace("-", "");

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .ownerId(owner.getOwnerId())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

            // ✅ 비밀번호 재설정 전용 메일 발송
            passwordResetMailSender.sendPasswordResetLink(
                    owner.getEmail(),
                    resetLink);
        });

        // ❗ 이메일 존재 여부는 절대 노출하지 않음 (보안)
    }

    /**
     * 2️⃣ 비밀번호 재설정 확정
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {

        // ✅ 입력 검증 (유저 실수 → PasswordResetException)
        if (token == null || token.isBlank()) {
            throw new PasswordResetException("유효하지 않은 링크입니다. 다시 요청해주세요.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new PasswordResetException("새 비밀번호를 입력해주세요.");
        }
        if (newPassword.length() < 8) {
            throw new PasswordResetException("비밀번호는 8자리 이상이어야 합니다.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordResetException("유효하지 않은 토큰입니다."));

        // ✅ 토큰 상태 검증 (정상 실패 → PasswordResetException)
        if (resetToken.isUsed()) {
            throw new PasswordResetException("이미 사용된 토큰입니다.");
        }
        if (resetToken.isExpired()) {
            throw new PasswordResetException("만료된 토큰입니다.");
        }

        Owner owner = ownerRepository.findById(resetToken.getOwnerId())
                .orElseThrow(() -> new PasswordResetException("계정을 찾을 수 없습니다."));

        // ✅ 기존 비밀번호와 동일하면 거부
        if (passwordEncoder.matches(newPassword, owner.getPassword())) {
            throw new PasswordResetException("기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.");
        }

        owner.setPassword(passwordEncoder.encode(newPassword));
        resetToken.markUsed();

        // ✅ 모든 기존 로그인 세션 무효화(사장 refreshToken)
        refreshTokenRepository.revokeAllByOwnerId(owner.getOwnerId());
    }
}