package com.erp.erp_back.controller.auth;

import com.erp.erp_back.dto.auth.OwnerLoginRequest;
import com.erp.erp_back.dto.auth.OwnerLoginResponse;
import com.erp.erp_back.service.auth.OwnerLoginService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/login")
public class OwnerLoginController {

    private final OwnerLoginService service;

    @PostMapping("/owner")
    public ResponseEntity<OwnerLoginResponse> ownerLogin(@Valid @RequestBody OwnerLoginRequest req) {
        return ResponseEntity.ok(service.login(req));
    }
}