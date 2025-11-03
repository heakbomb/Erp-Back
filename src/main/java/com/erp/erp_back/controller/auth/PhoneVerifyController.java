package com.erp.erp_back.controller.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.auth.PhoneVerifyRequestDto;
import com.erp.erp_back.dto.auth.PhoneVerifyResponseDto;
import com.erp.erp_back.dto.auth.PhoneVerifyStatusDto;
import com.erp.erp_back.service.auth.PhoneVerifyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/phone-verify") // (주의) /api/auth 아님
public class PhoneVerifyController {

    private final PhoneVerifyService phoneVerifyService;

    /**
     * 1. 인증 요청 API
     */
    @PostMapping("/request")
    public PhoneVerifyResponseDto handleAuthRequest(@RequestBody PhoneVerifyRequestDto requestDto) {
        return phoneVerifyService.requestVerification(requestDto);
    }

    /**
     * 2. 인증 상태 확인 API
     */
    @GetMapping("/status")
    public PhoneVerifyStatusDto getAuthStatus(@RequestParam("code") String code) {
        return phoneVerifyService.getVerificationStatus(code);
    }
}