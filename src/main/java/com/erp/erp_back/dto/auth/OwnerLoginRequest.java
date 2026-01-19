package com.erp.erp_back.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OwnerLoginRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}