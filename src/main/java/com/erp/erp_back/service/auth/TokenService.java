package com.erp.erp_back.service.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.auth.RefreshToken;
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
        RefreshToken rt = refreshTokenRepository
                .findValid(refreshToken, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("REFRESH_TOKEN_INVALID"));

        Claims claims = jwt.parse(refreshToken);

        String typ = (String) claims.get("typ");
        if (!"REFRESH".equals(typ)) {
            throw new IllegalArgumentException("REFRESH_TOKEN_INVALID");
        }

        Long ownerId = Long.valueOf(claims.getSubject());

        // role/email은 accessToken 생성에 필요하면 DB에서 조회해서 넣어도 됨.
        // 지금은 OwnerLoginService에서 email을 claims에 안 넣었으니,
        // accessToken은 최소 ownerId + role만 넣거나, owner 조회를 추가해도 됨.
        // 네 기존 createAccessToken 시 email 필요하므로 아래처럼 Owner 조회를 추가하는 게 안전.
        // (단, 최소 변경으로는 createAccessToken 시 email을 제외한 오버로드를 만들어도 됨)

        // ✅ 여기서는 email 없이 access 생성 오버로드를 추가하는게 가장 영향 적음.
        // => 아래 8번에서 JwtTokenProvider 오버로드 제공.

        return jwt.createAccessToken(ownerId, null, "OWNER"); // (오버로드로 처리)
    }
}