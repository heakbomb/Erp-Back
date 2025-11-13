package com.erp.erp_back.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserStatsResponse {
    private long totalUsers;
    private long totalOwners;
    private long totalEmployees;
    private long newSignupsThisMonth;
}