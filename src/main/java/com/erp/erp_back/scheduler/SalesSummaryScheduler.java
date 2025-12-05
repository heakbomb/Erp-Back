package com.erp.erp_back.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.SalesDailySummary;
import com.erp.erp_back.entity.erp.SalesMenuDailySummary;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.erp.DailyMenuStatDto; // 이전에 만든 인터페이스
import com.erp.erp_back.repository.erp.SalesDailySummaryRepository;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesMenuDailySummaryRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesSummaryScheduler {

    private final SalesTransactionRepository transactionRepo;
    private final SalesDailySummaryRepository summaryRepo;
    
    // 메뉴 요약용 리포지토리 추가 주입
    private final SalesLineItemRepository lineItemRepo;
    private final SalesMenuDailySummaryRepository menuSummaryRepo;

    // 매일 새벽 2시 실행
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void summarizeYesterdayData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[Scheduler] {} 일자 데이터 집계 시작", yesterday);
        
        // 1. 매출 요약 (Dashboard)
        summarizeSales(yesterday);
        
        // 2. 메뉴 판매 요약 (Top Menu)
        summarizeMenus(yesterday);
        
        log.info("[Scheduler] {} 일자 데이터 집계 완료", yesterday);
    }

    private void summarizeSales(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Map<String, Object>> stats = transactionRepo.findDailySalesStatsByDate(start, end);

        List<SalesDailySummary> summaries = stats.stream()
            .map(row -> SalesDailySummary.builder()
                .storeId((Long) row.get("storeId"))
                .summaryDate(date)
                .totalSales((BigDecimal) row.get("totalSales"))
                .transactionCount((Long) row.get("count"))
                .build())
            .toList();

        if (!summaries.isEmpty()) {
            summaryRepo.saveAll(summaries);
        }
        log.info(" - 매출 요약: {}개 매장 처리됨", summaries.size());
    }

    private void summarizeMenus(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        // SalesLineItemRepository에 findDailyMenuStatsByDate 메서드가 있어야 합니다.
        List<DailyMenuStatDto> stats = lineItemRepo.findDailyMenuStatsByDate(start, end);

        List<SalesMenuDailySummary> summaries = stats.stream()
            .map(dto -> SalesMenuDailySummary.builder()
                .store(Store.builder().storeId(dto.getStoreId()).build()) // Proxy 객체
                .menuItem(MenuItem.builder().menuId(dto.getMenuId()).build())
                .summaryDate(date)
                .totalQuantity(dto.getTotalQuantity())
                .totalAmount(dto.getTotalAmount())
                .build())
            .toList();

        if (!summaries.isEmpty()) {
            menuSummaryRepo.saveAll(summaries);
        }
        log.info(" - 메뉴 요약: {}건 데이터 생성됨", summaries.size());
    }
}