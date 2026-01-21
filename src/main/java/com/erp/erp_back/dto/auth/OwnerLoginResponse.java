package com.erp.erp_back.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerLoginResponse {
    private Long ownerId;
    private String email;
    private String username;
    private String accessToken;
    private String refreshToken;
}