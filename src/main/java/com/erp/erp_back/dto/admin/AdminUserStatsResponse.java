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
public class AdminUserStatsResponse {
    private long totalUsers;
    private long totalOwners;
    private long totalEmployees;
    private long newSignupsThisMonth;
}