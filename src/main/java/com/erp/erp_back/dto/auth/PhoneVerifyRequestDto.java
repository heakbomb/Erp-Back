package com.erp.erp_back.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhoneVerifyRequestDto {
    private String phoneNumber;
}