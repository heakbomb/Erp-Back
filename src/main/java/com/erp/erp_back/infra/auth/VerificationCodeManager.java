package com.erp.erp_back.infra.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeManager {

    private final SecureRandom random = new SecureRandom();

    @Value("${app.verification.code-secret}")
    private String codeSecret; // 서버 비밀키(pepper)

    public String generate6Digits() {
        int n = random.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    public String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((codeSecret + ":" + code).getBytes(StandardCharsets.UTF_8));
            return toHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("hash failure", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}