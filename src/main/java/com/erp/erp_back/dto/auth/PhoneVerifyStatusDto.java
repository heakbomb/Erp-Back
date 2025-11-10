package com.erp.erp_back.dto.auth;

import com.erp.erp_back.entity.enums.VerificationStatus;

import lombok.Getter;

@Getter
public class PhoneVerifyStatusDto {

    private String status;

    // Entity의 Enum을 DTO의 String으로 변환
    public PhoneVerifyStatusDto(VerificationStatus status) {
        this.status = status.toString();
    }

    // "NOT_FOUND" 등 커스텀 상태 응답
    public PhoneVerifyStatusDto(String status) {
        this.status = status;
    }
}
