// src/main/java/com/erp/erp_back/controller/erp/SalesController.java
package com.erp.erp_back.controller.erp;

import com.erp.erp_back.dto.erp.PosOrderRequest;
import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.SalesDailyStatResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.service.erp.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/owner/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/pos-order")
    public PosOrderResponse createPosOrder(@Valid @RequestBody PosOrderRequest req) {
        return salesService.createPosOrder(req);
    }

    // ✅ 그래프용: 기간별 매출
    @GetMapping("/daily")
    public List<SalesDailyStatResponse> getStatsByPeriod(
            @RequestParam Long storeId,
            @RequestParam(name = "period", defaultValue = "DAY") String period
    ) {
        return salesService.getStatsByPeriod(storeId, period);
    }

    // ✅ 메뉴별 분석용 TOP5
    @GetMapping("/top-menus")
    public List<TopMenuStatsResponse> getTopMenus(
            @RequestParam Long storeId,
            @RequestParam(name = "period", defaultValue = "DAY") String period
    ) {
        return salesService.getTopMenusByPeriod(storeId, period);
    }

    @GetMapping("/transactions")
    public List<SalesTransactionSummaryResponse> getTransactionsByRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return salesService.getTransactionsByRange(storeId, from, to);
    }
    
     @GetMapping("/summary")
    public SalesSummaryResponse getSalesSummary(@RequestParam Long storeId) {
        return salesService.getSalesSummary(storeId);
    }
}
