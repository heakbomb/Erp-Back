package com.erp.erp_back.controller.ai;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.ai.DemandForecastResponse;
import com.erp.erp_back.dto.ai.MenuGrowthResponse;
import com.erp.erp_back.service.ai.AiDataService;

import lombok.RequiredArgsConstructor;

@RestController
// ✅ 경로 수정: /api 제거
@RequestMapping("/ai/insights")
@RequiredArgsConstructor
public class AiInsightController {

    private final AiDataService aiDataService;

    // 호출 URL: GET /ai/insights/{storeId}/forecast
    @GetMapping("/{storeId}/forecast")
    public ResponseEntity<List<DemandForecastResponse>> getDemandForecast(@PathVariable Long storeId) {
        List<DemandForecastResponse> response = aiDataService.getWeeklyForecast(storeId);
        return ResponseEntity.ok(response);
    }

    // ✅ [추가] 메뉴 트렌드 분석 API
    @GetMapping("/{storeId}/menu-growth")
    public ResponseEntity<List<MenuGrowthResponse>> getMenuGrowth(@PathVariable Long storeId) {
        List<MenuGrowthResponse> response = aiDataService.getMenuGrowthAnalysis(storeId);
        return ResponseEntity.ok(response);
    }
}