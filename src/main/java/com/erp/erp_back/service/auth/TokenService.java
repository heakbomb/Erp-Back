package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.repository.auth.RefreshTokenRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwt;

    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {

        // ✅ 존재 + 만료 + revoke 검증용 (변수 불필요)
        refreshTokenRepository
                .findValid(refreshToken, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("REFRESH_TOKEN_INVALID"));

        Claims claims = jwt.parse(refreshToken);

        String typ = (String) claims.get("typ");
        if (!"REFRESH".equals(typ)) {
            throw new IllegalArgumentException("REFRESH_TOKEN_INVALID");
        }

        Long ownerId = Long.valueOf(claims.getSubject());

        // 기존 설계 유지
        return jwt.createAccessToken(ownerId, null, "OWNER");
    }
}