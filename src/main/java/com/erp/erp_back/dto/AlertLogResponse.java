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
public class AlertLogResponse {
    private Long alertId;
    private Long storeId;
    private String alertType;
    private String severity;
    private LocalDateTime createdAt;
}