package com.erp.erp_back.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OwnerRegisterRequest {
    @NotBlank
    private String username;

    @Email @NotBlank
    private String email;

    // bcrypt 기준 72byte 권장(문자 수는 72 이하가 안전)
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    private String verificationId;
}