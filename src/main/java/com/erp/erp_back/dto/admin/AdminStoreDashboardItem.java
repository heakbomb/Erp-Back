package com.erp.erp_back.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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