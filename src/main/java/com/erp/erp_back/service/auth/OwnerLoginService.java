package com.erp.erp_back.service.auth;

import com.erp.erp_back.dto.auth.OwnerLoginRequest;
import com.erp.erp_back.dto.auth.OwnerLoginResponse;
import com.erp.erp_back.entity.auth.RefreshToken;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.infra.auth.jwt.JwtProperties;
import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.repository.auth.RefreshTokenRepository;
import com.erp.erp_back.repository.user.OwnerRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerLoginService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public OwnerLoginResponse login(OwnerLoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.getPassword(), owner.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwt.createAccessToken(owner.getOwnerId(), owner.getEmail(), "OWNER");
        String refreshToken = jwt.createRefreshToken(owner.getOwnerId());

        // ✅ (권장) 로그인 시 기존 refresh 전부 revoke (단일 디바이스 정책)
        refreshTokenRepository.revokeAllByOwnerId(owner.getOwnerId());

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(jwtProps.refreshTokenTtlDays());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .owner(owner)
                        .token(refreshToken)
                        .expiresAt(expiresAt)
                        .revoked(false)
                        .build());

        return new OwnerLoginResponse(
                owner.getOwnerId(),
                owner.getEmail(),
                owner.getUsername(),
                token,
                refreshToken);
    }
}