package com.erp.erp_back.dto.log;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long auditId;
    private Long userId; 
    private String userType; 
    private String actionType; 
    private String targetTable; 
    private LocalDateTime createdAt;
}