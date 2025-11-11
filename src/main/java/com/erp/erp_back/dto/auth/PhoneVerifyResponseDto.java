package com.erp.erp_back.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneVerifyResponseDto {
    private final String authCode;
}