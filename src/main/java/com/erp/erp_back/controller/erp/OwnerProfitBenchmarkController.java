package com.erp.erp_back.controller.erp;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * /owner/analysis/profit-benchmark?storeId=101&year=2026&month=1&myCogsRate=0.30
     */
    @GetMapping("/profit-benchmark")
    public ProfitBenchmarkResponse profitBenchmark(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam BigDecimal myCogsRate
    ) {
        return service.getMonthlyProfitBenchmark(storeId, year, month, myCogsRate);
    }
}
