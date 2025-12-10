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
    @Scheduled(cron = "0 40 20 * * ?")
    @Transactional
    public void summarizeYesterdayData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate targetFrom = findLastSummaryDatePlusOneOrFallback(yesterday.minusDays(7));
        LocalDate targetTo = yesterday;

        log.info("[Scheduler] {} ~ {} 일자 데이터 집계 시작", targetFrom, targetTo);

        LocalDate cur = targetFrom;
        while (!cur.isAfter(targetTo)) {
            summarizeSales(cur);
            summarizeMenus(cur);
            cur = cur.plusDays(1);
        }

        log.info("[Scheduler] {} ~ {} 일자 데이터 집계 완료", targetFrom, targetTo);
    }

    private LocalDate findLastSummaryDatePlusOneOrFallback(LocalDate fallbackFrom) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate maxDate = summaryRepo.findMaxSummaryDate(); // 가장 최근 요약 날짜

        if (maxDate == null) {
            // ▶ 요약 데이터가 하나도 없으면: fallbackFrom부터 시작
            return fallbackFrom;
        }

        // ▶ 마지막 요약 날짜의 다음 날부터 다시 집계 시작
        LocalDate candidate = maxDate.plusDays(1);

        // 만약 이미 어제까지 다 요약돼 있으면, 어제 하루만 대상으로 삼도록 조정
        if (candidate.isAfter(yesterday)) {
            return yesterday;
        }

        // candidate가 fallbackFrom보다 이전이면 fallbackFrom부터 시작
        return candidate.isBefore(fallbackFrom) ? fallbackFrom : candidate;
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