package com.erp.erp_back.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordResetConfirmDto {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}