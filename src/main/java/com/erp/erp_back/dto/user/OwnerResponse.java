package com.erp.erp_back.dto.user;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {
    private Long ownerId;
    private String username;
    private String email;
    private LocalDateTime createdAt; 
}