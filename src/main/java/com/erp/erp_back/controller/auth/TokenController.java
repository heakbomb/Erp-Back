package com.erp.erp_back.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.auth.RefreshRequest;
import com.erp.erp_back.dto.auth.RefreshResponse;
import com.erp.erp_back.service.auth.TokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/token")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String accessToken = tokenService.refreshAccessToken(req.refreshToken());
        return ResponseEntity.ok(new RefreshResponse(accessToken));
    }
}