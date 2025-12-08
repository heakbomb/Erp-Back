package com.erp.erp_back.dto.admin;

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
public class DashboardStatsResponse {
    // 1. 카드 4개
    private long totalStores;
    private long totalUsers;
    private long activeSubscriptions; // ✅ 활성 구독
    private long pendingStoreCount; // ✅ 승인 대기 사업장
    private Long pendingInquiryCount; // 대기 문의
}
