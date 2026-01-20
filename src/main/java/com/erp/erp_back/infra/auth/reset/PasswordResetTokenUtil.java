package com.erp.erp_back.infra.auth.reset;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    // raw token = hex string
    public String generateRawToken(int bytes) {
        byte[] b = new byte[bytes];
        SECURE_RANDOM.nextBytes(b);
        return HEX.formatHex(b);
    }

    public String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}