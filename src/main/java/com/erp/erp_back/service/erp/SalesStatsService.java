package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.DashboardStatsProjection;
import com.erp.erp_back.dto.erp.SalesDailyStatResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.mapper.SalesMapper;
import com.erp.erp_back.repository.erp.SalesDailySummaryRepository;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesMenuDailySummaryRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.util.DateRangeUtils;
import com.erp.erp_back.util.DateRangeUtils.DateRange;
import static com.erp.erp_back.util.SalesCalcUtils.calcAvgTicket;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesStatsService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesDailySummaryRepository salesDailySummaryRepository; 
    private final SalesLineItemRepository salesLineItemRepository;
    private final SalesMapper salesMapper;
    private final SalesMenuDailySummaryRepository salesMenuDailySummaryRepository;

    /**
     * [메인 변경] 하이브리드 대시보드 요약 (성능 최적화됨)
     */
public SalesSummaryResponse getSalesSummary(Long storeId) {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    // 1. 날짜 범위 정의
    DateRange weekRange = DateRangeUtils.forThisWeek(today);
    DateRange prevWeekRange = new DateRange(
            weekRange.start().minusWeeks(1),
            weekRange.end().minusWeeks(1)
    );

    DateRange monthRange = DateRangeUtils.forThisMonth(today);
    DateRange prevMonthRange = new DateRange(
            monthRange.start().minusMonths(1),
            monthRange.end().minusMonths(1)
    );

    // 쿼리 최적화용: 가장 오래된 조회 시작 날짜
    LocalDate minDate = prevMonthRange.start().toLocalDate();
    if (prevWeekRange.start().toLocalDate().isBefore(minDate)) {
        minDate = prevWeekRange.start().toLocalDate();
    }

    // =================================================================
    // STEP 1: 과거 데이터 조회 (요약 테이블 - 1번의 쿼리로 모든 과거 데이터 조회)
    // =================================================================
    DashboardStatsProjection pastStats = salesDailySummaryRepository.findIntegratedStats(
            storeId,
            yesterday,
            weekRange.start().toLocalDate(),     // 이번주 시작
            prevWeekRange.start().toLocalDate(), // 지난주 시작
            prevWeekRange.end().toLocalDate(),   // 지난주 끝
            monthRange.start().toLocalDate(),    // 이번달 시작
            prevMonthRange.start().toLocalDate(),// 지난달 시작
            prevMonthRange.end().toLocalDate(),  // 지난달 끝
            minDate
    );

    // Null 방지 (데이터가 없으면 0)
    BigDecimal pastYesterday = safe(pastStats != null ? pastStats.yesterdaySales() : null);
    BigDecimal pastThisWeek  = safe(pastStats != null ? pastStats.thisWeekSales() : null);
    BigDecimal pastLastWeek  = safe(pastStats != null ? pastStats.lastWeekSales() : null);
    BigDecimal pastThisMonth = safe(pastStats != null ? pastStats.thisMonthSales() : null);
    BigDecimal pastLastMonth = safe(pastStats != null ? pastStats.lastMonthSales() : null);

    // =================================================================
    // STEP 2: 오늘 실시간 데이터 조회 (Transaction 테이블 - 오늘치만 스캔)
    // =================================================================
    BigDecimal todaySales = safe(
            salesTransactionRepository.sumTotalAmountByStoreIdBetween(
                    storeId,
                    today.atStartOfDay(),
                    LocalDateTime.now()
            )
    );

    // =================================================================
    // STEP 3: 데이터 병합 (과거 + 오늘) + 요약 없을 때 fallback
    // =================================================================

    // 1. 일별: [오늘] vs [어제]
    BigDecimal currentDay = todaySales;
    BigDecimal prevDay    = pastYesterday;

    // 2. 주간
    BigDecimal currentWeek;
    if (pastThisWeek.compareTo(BigDecimal.ZERO) > 0) {
        // ✅ 요약 테이블에 이번주(어제까지) 데이터가 있으면: 요약 + 오늘
        currentWeek = pastThisWeek.add(todaySales);
    } else {
        // ⚠ 요약이 비어 있으면: 이번 주 전체를 트랜잭션에서 직접 합산
        currentWeek = safe(
                salesTransactionRepository.sumTotalAmountByStoreIdBetween(
                        storeId,
                        weekRange.start(),            // 이번주 시작 (예: 월요일 00:00)
                        LocalDateTime.now()           // 현재 시각
                )
        );
    }
    BigDecimal prevWeek = pastLastWeek;            // 지난주는 여전히 요약 사용

    // 3. 월간
    BigDecimal currentMonth;
    if (pastThisMonth.compareTo(BigDecimal.ZERO) > 0) {
        // ✅ 요약 테이블에 이번달(어제까지) 데이터가 있으면: 요약 + 오늘
        currentMonth = pastThisMonth.add(todaySales);
    } else {
        // ⚠ 요약이 비어 있으면: 이번 달 전체를 트랜잭션에서 직접 합산
        currentMonth = safe(
                salesTransactionRepository.sumTotalAmountByStoreIdBetween(
                        storeId,
                        monthRange.start(),           // 이번달 1일 00:00
                        LocalDateTime.now()
                )
        );
    }
    BigDecimal prevMonth = pastLastMonth;          // 지난달은 요약 사용

    // 4. 객단가 (currentMonth 기준으로 그대로 계산)
    Long currentMonthCountRaw =
            salesTransactionRepository.countByStoreStoreIdAndTransactionTimeBetween(
                    storeId,
                    monthRange.start(),
                    LocalDateTime.now()
            );

    Long prevMonthCountRaw =
            salesTransactionRepository.countByStoreStoreIdAndTransactionTimeBetween(
                    storeId,
                    prevMonthRange.start(),
                    prevMonthRange.end()
            );

    long currentMonthCount = safeCount(currentMonthCountRaw);
    long prevMonthCount    = safeCount(prevMonthCountRaw);

    BigDecimal avgTicket     = calcAvgTicket(currentMonth, currentMonthCount);
    BigDecimal prevAvgTicket = calcAvgTicket(prevMonth,   prevMonthCount);

    return salesMapper.toSalesSummary(
            currentDay, prevDay,
            currentWeek, prevWeek,
            currentMonth, prevMonth,
            avgTicket, prevAvgTicket
    );
}


    private BigDecimal safe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    // =============================================================
    // 아래 메서드들은 기존 그래프/목록 조회를 위해 유지합니다.
    // =============================================================

    public List<SalesDailyStatResponse> getSalesStats(Long storeId, LocalDate from, LocalDate to) {
        DateRange range = DateRangeUtils.between(from, to);
        List<Map<String, Object>> rows = salesTransactionRepository.findDailySalesStats(storeId, range.start(), range.end());
        return rows.stream()
                .map(row -> new SalesDailyStatResponse(
                        (String) row.get("date"),
                        (BigDecimal) row.getOrDefault("sales", BigDecimal.ZERO)
                ))
                .collect(Collectors.toList());
    }

    public List<SalesDailyStatResponse> getDailyStats(Long storeId, LocalDate from, LocalDate to) {
        return getSalesStats(storeId, from, to);
    }

    // [수정됨] 인기 메뉴 조회 (하이브리드 방식)
    public List<TopMenuStatsResponse> getTopMenus(Long storeId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        List<TopMenuStatsResponse> mergedStats = new ArrayList<>();

        // 1. 과거 데이터 조회 (어제까지)
        if (from.isBefore(today)) {
            LocalDate pastEnd = to.isBefore(today) ? to : today.minusDays(1);
            List<TopMenuStatsResponse> pastData = salesMenuDailySummaryRepository.findTopMenuStats(storeId, from, pastEnd);
            mergedStats.addAll(pastData);
        }

        // 2. 오늘 데이터 조회 (오늘 포함 시)
        if (!to.isBefore(today)) {
            // 오늘 하루치는 데이터 양이 적으므로 기존 SalesLineItemRepository 사용
            // (주의: SalesLineItemRepository 메서드도 LocalDateTime을 받도록 파라미터 확인 필요)
            List<TopMenuStatsResponse> todayData = salesLineItemRepository.findTopMenuStats(
                storeId, today.atStartOfDay(), LocalDateTime.now()
            );
            mergedStats.addAll(todayData);
        }

        // 3. 병합 및 정렬 (메모리 연산)
        return mergedStats.stream()
        .collect(Collectors.toMap(
            TopMenuStatsResponse::getMenuId, 
            Function.identity(), 
            (a, b) -> new TopMenuStatsResponse(
                a.getMenuId(), 
                a.getMenuName(), 
                a.getQuantity() + b.getQuantity(), 
                a.getRevenue().add(b.getRevenue())
            )
        ))
        .values().stream()
        .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue())) // 매출액 내림차순
        .limit(5) // 상위 5개
        .toList(); // ✅ 바로 반환
    }
    
    public List<TopMenuStatsResponse> getTodayTopMenus(Long storeId) {
        LocalDate today = LocalDate.now();
        return getTopMenus(storeId, today, today);
    }

    public Page<SalesTransactionSummaryResponse> getTransactions(Long storeId, LocalDate from, LocalDate to, Pageable pageable) {
        DateRange range = DateRangeUtils.between(from, to);
        Page<SalesTransaction> page = salesTransactionRepository
                .findByStoreStoreIdAndTransactionTimeBetween(storeId, range.start(), range.end(), pageable);
        return page.map(salesMapper::toSummaryResponse);
    }

    private long safeCount(Long val) {
        return val == null ? 0L : val;
    }
}