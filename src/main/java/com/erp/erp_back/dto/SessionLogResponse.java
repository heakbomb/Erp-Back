package com.erp.erp_back.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionLogResponse {
    private Long sessionId;
    private Long ownerId;
    private String userType; 
    private LocalDateTime loginTime;
    // refresh_token은 응답에 포함하지 않습니다.
}