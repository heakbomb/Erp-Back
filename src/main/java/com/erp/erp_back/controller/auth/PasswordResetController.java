package com.erp.erp_back.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.service.auth.PasswordResetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset/request")
    public ResponseEntity<Void> request(@RequestParam String email) {
        passwordResetService.requestReset(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<Void> confirm(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        passwordResetService.resetPassword(token, newPassword);
        return ResponseEntity.noContent().build();
    }
}