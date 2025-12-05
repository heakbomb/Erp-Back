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
        // ✅ 수정: between() 대신 new DateRange() 사용
        DateRange prevWeekRange = new DateRange(
                weekRange.start().minusWeeks(1), 
                weekRange.end().minusWeeks(1)
        );
        
        DateRange monthRange = DateRangeUtils.forThisMonth(today);
        // ✅ 수정: between() 대신 new DateRange() 사용
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
        BigDecimal todaySales = salesTransactionRepository.sumTotalAmountByStoreIdBetween(
            storeId, today.atStartOfDay(), LocalDateTime.now()
        );
        todaySales = safe(todaySales);


        // =================================================================
        // STEP 3: 데이터 병합 (과거 + 오늘)
        // =================================================================
        
        // 1. 일별: [오늘] vs [어제]
        BigDecimal currentDay = todaySales;
        BigDecimal prevDay    = pastYesterday;

        // 2. 주간: [오늘 + 이번주(어제까지)] vs [지난주]
        BigDecimal currentWeek = pastThisWeek.add(todaySales);
        BigDecimal prevWeek    = pastLastWeek;

        // 3. 월간: [오늘 + 이번달(어제까지)] vs [지난달]
        BigDecimal currentMonth = pastThisMonth.add(todaySales);
        BigDecimal prevMonth    = pastLastMonth;

        // 4. 객단가 (단순화를 위해 이번달 기준만 예시로 계산, 필요 시 Count도 가져와서 정확히 계산 가능)
        // 여기서는 기존 로직과 호환성을 위해 0으로 두거나 별도 Count 로직 추가 가능
        // 성능을 위해 일단 금액 위주로 구성합니다.
        BigDecimal avgTicket = BigDecimal.ZERO; 
        BigDecimal prevAvgTicket = BigDecimal.ZERO;

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
}