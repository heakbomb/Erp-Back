package com.erp.erp_back.infra.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long ownerId, String emailOrNull, String role) {
        long now = System.currentTimeMillis();
        long exp = now + props.accessTokenTtlMinutes() * 60_000;

        var builder = Jwts.builder()
                .subject(String.valueOf(ownerId))
                .claims(Map.of(
                        "role", role,
                        "typ", "ACCESS"))
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key);

        if (emailOrNull != null) {
            builder.claim("email", emailOrNull);
        }

        return builder.compact();
    }

    // ✅ refresh token 발급 (typ=REFRESH)
    public String createRefreshToken(Long ownerId) {
        long now = System.currentTimeMillis();
        long exp = now + props.refreshTokenTtlDays() * 24L * 60L * 60L * 1000L;

        return Jwts.builder()
                .subject(String.valueOf(ownerId))
                .claims(Map.of("typ", "REFRESH"))
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}