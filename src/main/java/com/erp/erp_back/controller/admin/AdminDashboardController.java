package com.erp.erp_back.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.admin.DashboardStatsResponse;
import com.erp.erp_back.service.admin.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/dashboard") // ✅ API 루트
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * (Admin) 대시보드 통계 데이터 조회
     * GET /admin/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}