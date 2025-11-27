package com.erp.erp_back.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor 
@NoArgsConstructor
public class AdminStoreDashboardItem {
    private Long storeId;
    private String storeName;
    private String industry;
    private String status;
    private long employeeCount; // 직원 수 (집계)
    private BigDecimal totalSalesMonth; // 당월 매출 (집계)
    private LocalDateTime lastSalesDate; // 최근 매출일

    private String ownerName;
    private String ownerEmail;
    private String bizNum; // 필드 추가
}