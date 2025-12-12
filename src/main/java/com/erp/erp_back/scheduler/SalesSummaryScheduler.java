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

        // 1) 원본 통계 조회
        List<Map<String, Object>> rawStats = transactionRepo.findDailySalesStatsByDate(start, end);

        // 2) 같은 매장(storeId) 기준으로 Java에서 한 번 더 합치기 (중복 row 대비)
        Map<Long, AggregatedDailyStat> statByStore = new java.util.HashMap<>();

        for (Map<String, Object> row : rawStats) {
            Long storeId = (Long) row.get("storeId");
            BigDecimal totalSales = (BigDecimal) row.get("totalSales");
            Long count = (Long) row.get("count");

            AggregatedDailyStat agg = statByStore.computeIfAbsent(
                    storeId,
                    id -> new AggregatedDailyStat(BigDecimal.ZERO, 0L));

            // null 방어하면서 합계 누적
            agg.totalSales = agg.totalSales.add(totalSales != null ? totalSales : BigDecimal.ZERO);
            agg.transactionCount = agg.transactionCount + (count != null ? count : 0L);
        }

        int affectedStores = 0;

        // 3) 매장별로 1번씩만 upsert (delete 후 insert)
        for (Map.Entry<Long, AggregatedDailyStat> entry : statByStore.entrySet()) {
            Long storeId = entry.getKey();
            AggregatedDailyStat agg = entry.getValue();

            // ✅ 먼저 기존 요약 삭제 (없으면 0건 삭제라 괜찮음)
            summaryRepo.deleteByStoreIdAndSummaryDate(storeId, date);

            // ✅ 새 row 생성해서 INSERT (PK는 자동 생성)
            SalesDailySummary fresh = SalesDailySummary.builder()
                    .storeId(storeId)
                    .summaryDate(date)
                    .totalSales(agg.totalSales)
                    .transactionCount(agg.transactionCount)
                    .build();

            summaryRepo.save(fresh); // 여기서는 중복 키가 날 수 없음

            affectedStores++;
        }

        log.info(" - 매출 요약: {}개 매장 처리 완료 (date={})", affectedStores, date);
    }

    private void summarizeMenus(LocalDate date) {
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(23, 59, 59);

    // ✅ 1) 이 날짜에 대한 기존 메뉴 요약 싹 삭제 (모든 매장/메뉴)
    menuSummaryRepo.deleteBySummaryDate(date);

    // ✅ 2) 원본 LineItem에서 통계 뽑기
    List<DailyMenuStatDto> stats = lineItemRepo.findDailyMenuStatsByDate(start, end);

    List<SalesMenuDailySummary> summaries = stats.stream()
            .map(dto -> SalesMenuDailySummary.builder()
                    .store(Store.builder().storeId(dto.getStoreId()).build()) // Proxy
                    .menuItem(MenuItem.builder().menuId(dto.getMenuId()).build())
                    .summaryDate(date)
                    .totalQuantity(dto.getTotalQuantity())
                    .totalAmount(dto.getTotalAmount())
                    .build())
            .toList();

    // ✅ 3) 새 데이터만 INSERT
    if (!summaries.isEmpty()) {
        menuSummaryRepo.saveAll(summaries);
    }

    log.info(" - 메뉴 요약: {}건 데이터 생성됨 (date={})", summaries.size(), date);
}

    private static class AggregatedDailyStat {
        BigDecimal totalSales;
        Long transactionCount;

        AggregatedDailyStat(BigDecimal totalSales, Long transactionCount) {
            this.totalSales = totalSales;
            this.transactionCount = transactionCount;
        }
    }
}
