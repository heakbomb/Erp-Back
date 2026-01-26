package com.erp.erp_back.controller.erp;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.erp.ProfitBenchmarkResponse;
import com.erp.erp_back.service.erp.ProfitBenchmarkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/analysis")
public class OwnerProfitBenchmarkController {

    private final ProfitBenchmarkService service;

    /**
     * 예:
     * /owner/analysis/profit-benchmark?storeId=101&year=2026&month=1
     * /owner/analysis/profit-benchmark?storeId=101&year=2026&month=1&myCogsRate=0.30
     */
    @GetMapping("/profit-benchmark")
    public ProfitBenchmarkResponse profitBenchmark(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) BigDecimal myCogsRate
    ) {
        // ✅ 대충 평균이면 null일 때 디폴트 (원하면 업종별로 더 정교하게 가능)
        if (myCogsRate == null) myCogsRate = BigDecimal.valueOf(0.33);

        return service.getMonthlyProfitBenchmark(storeId, year, month, myCogsRate);
    }
}
