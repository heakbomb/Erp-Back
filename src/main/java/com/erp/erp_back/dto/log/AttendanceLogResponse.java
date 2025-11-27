package com.erp.erp_back.dto.log;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLogResponse {
    private Long logId;
    private Long employeeId;
    private Long storeId;
    private LocalDateTime recordTime;
    private String recordType;
}