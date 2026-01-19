package com.erp.erp_back.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OwnerLoginResponse {
    private Long ownerId;
    private String email;
    private String username;
    private String accessToken;
    private String refreshToken;
}