package com.erp.erp_back.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.auth.AdminLoginRequest; // ✅ 교체
import com.erp.erp_back.dto.auth.OwnerLoginResponse;
import com.erp.erp_back.service.auth.AdminLoginService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminLoginService adminLoginService;

    @PostMapping("/login")
    public ResponseEntity<OwnerLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) { // ✅ DTO 변경
        OwnerLoginResponse response = adminLoginService.login(request);
        return ResponseEntity.ok(response);
    }
}