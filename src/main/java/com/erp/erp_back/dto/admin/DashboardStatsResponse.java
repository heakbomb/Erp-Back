package com.erp.erp_back.dto.admin;

import java.util.List;

import com.erp.erp_back.dto.log.AuditLogResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {
    // 1. 카드 4개
    private long totalStores;
    private long totalUsers;
    private long activeSubscriptions; // ✅ 활성 구독
    private long pendingStoreCount; // ✅ 승인 대기 사업장
    
    // 2. 최근 활동
    private List<AuditLogResponse> recentActivities; // ✅ 최근 활동
}
