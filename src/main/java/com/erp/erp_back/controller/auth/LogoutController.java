package com.erp.erp_back.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.service.auth.LogoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        // JwtAuthFilter에서 principal = ownerId(subject) 넣고 있음
        Long ownerId = Long.valueOf(String.valueOf(authentication.getPrincipal()));
        logoutService.logout(ownerId);
        return ResponseEntity.noContent().build();
    }
}