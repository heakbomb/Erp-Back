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
public class OnboardingDataResponse {
    private Long onboardingId;
    private Long userId;
    private String dataType;
    private String rawJsonData;
    private LocalDateTime uploadTime;
}