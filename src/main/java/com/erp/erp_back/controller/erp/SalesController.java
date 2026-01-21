package com.erp.erp_back.controller.erp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.erp.MenuAnalyticsResponse;
import com.erp.erp_back.dto.erp.MonthlySalesReportResponse;
import com.erp.erp_back.dto.erp.PosOrderRequest;
import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.RefundRequest;
import com.erp.erp_back.dto.erp.SalesDailyStatResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.dto.erp.WeeklyAreaAvgResponse;
import com.erp.erp_back.service.erp.OwnerWeeklyAreaAvgService;
import com.erp.erp_back.service.erp.SalesReportService;
import com.erp.erp_back.service.erp.SalesService;
import com.erp.erp_back.service.erp.SalesStatsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // 필요시 설정 유지
public class SalesController {

    private final SalesService salesService;
    private final SalesStatsService salesStatsService;
    private final SalesReportService salesReportService;
    private final OwnerWeeklyAreaAvgService weeklyService;

    @PostMapping("/pos-order")
    public ResponseEntity<PosOrderResponse> createPosOrder(@Valid @RequestBody PosOrderRequest req) {
        PosOrderResponse res = salesService.createPosOrder(req);
        return ResponseEntity.ok(res);
    }

    /**
     * [결제 취소 / 환불]
     * - RequestBody로 사유와 폐기 여부를 받음
     */
    @PostMapping("/refund")
    public ResponseEntity<PosOrderResponse> refundOrder(@RequestBody RefundRequest req) {
        PosOrderResponse res = salesService.refundPosOrder(req);
        return ResponseEntity.ok(res);
    }

    /**
     * [차트용] 기간별 매출 데이터
     * - 프론트에서 from, to를 계산해서 보내줘야 함 (period 문자열 사용 X)
     */
    @GetMapping("/daily")
    public ResponseEntity<List<SalesDailyStatResponse>> getSalesStats(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesStatsService.getSalesStats(storeId, from, to));
    }

    /**
     * [인기 메뉴] 기간별 TOP 5
     */
    @GetMapping("/top-menus")
    public ResponseEntity<List<TopMenuStatsResponse>> getTopMenus(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesStatsService.getTopMenus(storeId, from, to));
    }

    /**
     * [대시보드 요약] 오늘/주간/월간 매출 및 비교 데이터
     */
    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(@RequestParam Long storeId) {
        return ResponseEntity.ok(salesStatsService.getSalesSummary(storeId));
    }

    /**
     * [거래 내역 조회] (페이징 적용)
     * - 대시보드용: ?page=0&size=20&sort=transactionTime,desc
     * - 상세페이지: ?page=0&size=10&sort=transactionTime,desc
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<SalesTransactionSummaryResponse>> getTransactions(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "transactionTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(salesStatsService.getTransactions(storeId, from, to, pageable));
    }

    @GetMapping("/stores/{storeId}/reports/monthly")
    public MonthlySalesReportResponse getMonthlyReport(
            @PathVariable Long storeId,
            @RequestParam int year,
            @RequestParam int month) {
        return salesReportService.getMonthlyReport(storeId, year, month);
    }

    @GetMapping("/weekly-area-avg")
    public ResponseEntity<WeeklyAreaAvgResponse> weeklyAreaAvg(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(weeklyService.getWeeklyAreaAvg(storeId, year, month));
    }

    @GetMapping("/menu-analytics")
    public MenuAnalyticsResponse menuAnalytics(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return salesStatsService.getMenuAnalytics(storeId, from, to);
    }
}