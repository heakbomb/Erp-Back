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
public class AttendanceLogResponse {
    private Long logId;
    private Long employeeId;
    private Long storeId;
    private LocalDateTime recordTime;
    private String recordType;
    private String clientIp;
}