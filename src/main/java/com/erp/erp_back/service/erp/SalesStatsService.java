package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.dto.erp.SalesDailyStatResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.enums.SalesPeriod;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.mapper.SalesMapper;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.util.DateRangeUtils;
import com.erp.erp_back.util.DateRangeUtils.DateRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesStatsService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesLineItemRepository salesLineItemRepository;
    private final SalesMapper salesMapper;

    public List<SalesDailyStatResponse> getSalesStats(Long storeId, LocalDate from, LocalDate to) {
        DateRange range = DateRangeUtils.between(from, to);

        // Repository는 일별로 Group By 된 결과(Map)를 반환
        List<Map<String, Object>> rows = salesTransactionRepository.findDailySalesStats(storeId, range.start(), range.end());

        return rows.stream()
                .map(row -> new SalesDailyStatResponse(
                        (String) row.get("date"),
                        (BigDecimal) row.getOrDefault("sales", BigDecimal.ZERO)
                ))
                .collect(Collectors.toList());
    }

    /**
     * [편의 메서드] 일별 매출 통계
     */
    public List<SalesDailyStatResponse> getDailyStats(Long storeId, LocalDate from, LocalDate to) {
        return getSalesStats(storeId, from, to);
    }

    public List<TopMenuStatsResponse> getTopMenus(Long storeId, LocalDate from, LocalDate to) {
        DateRange range = DateRangeUtils.between(from, to);

        // DTO Projection 사용 (Object[] 캐스팅 제거)
        List<TopMenuStatsResponse> stats = salesLineItemRepository.findTopMenuStats(storeId, range.start(), range.end());

        return stats.stream()
                .limit(5)
                .map(dto -> salesMapper.toTopMenuStats(
                        dto.getMenuId(),
                        dto.getMenuName(),
                        dto.getQuantity(),
                        dto.getRevenue(),
                        0.0 // 성장률 등 UI 계산 로직은 프론트로 위임
                ))
                .toList();
    }
    
    /**
     * [편의 메서드] 오늘 인기 메뉴
     */
    public List<TopMenuStatsResponse> getTodayTopMenus(Long storeId) {
        LocalDate today = LocalDate.now();
        return getTopMenus(storeId, today, today);
    }

    public SalesSummaryResponse getSalesSummary(Long storeId) {
        LocalDate today = LocalDate.now();

        // 1. 기간 정의 (현재 기준)
        DateRange todayRange = DateRangeUtils.forToday();
        DateRange weekRange  = DateRangeUtils.forThisWeek(today);
        DateRange monthRange = DateRangeUtils.forThisMonth(today);

        // 2. 매출 비교 (현재 vs 과거) - 헬퍼 메서드 활용
        SalesComparison daily   = compareSales(storeId, todayRange, SalesPeriod.DAY);
        SalesComparison weekly  = compareSales(storeId, weekRange,  SalesPeriod.WEEK);
        SalesComparison monthly = compareSales(storeId, monthRange, SalesPeriod.MONTH);

        // 3. 객단가 비교
        BigDecimal thisMonthAvg = calcAvgTicket(storeId, monthRange);
        BigDecimal lastMonthAvg = calcAvgTicket(storeId, getPreviousRange(monthRange, SalesPeriod.MONTH));

        return salesMapper.toSalesSummary(
                daily.current, daily.prev,
                weekly.current, weekly.prev,
                monthly.current, monthly.prev,
                thisMonthAvg, lastMonthAvg
        );
    }

public Page<SalesTransactionSummaryResponse> getTransactions(
            Long storeId, 
            LocalDate from, 
            LocalDate to, 
            Pageable pageable) {

        DateRange range = DateRangeUtils.between(from, to);

        Page<SalesTransaction> page = salesTransactionRepository
                .findByStoreStoreIdAndTransactionTimeBetween(
                        storeId, 
                        range.start(), 
                        range.end(), 
                        pageable
                );

        return page.map(salesMapper::toSummaryResponse);
    }

    private BigDecimal sumSales(Long storeId, DateRange range) {
        BigDecimal v = salesTransactionRepository.sumTotalAmountByStoreIdBetween(
                storeId, range.start(), range.end());
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal calcAvgTicket(Long storeId, DateRange range) {
        long count = salesTransactionRepository
                .countByStoreStoreIdAndTransactionTimeBetween(
                        storeId, range.start(), range.end());

        if (count == 0) return BigDecimal.ZERO;

        BigDecimal total = sumSales(storeId, range);
        return total.divide(BigDecimal.valueOf(count), 0, RoundingMode.DOWN);
    }

    // 내부에서만 쓸 간단한 데이터 홀더 (Java 16+ Record 추천)
    private record SalesComparison(BigDecimal current, BigDecimal prev) {}

    // 매출 비교 헬퍼: 현재 기간과 과거 기간의 매출을 한 번에 구해옴
    private SalesComparison compareSales(Long storeId, DateRange current, SalesPeriod period) {
        DateRange prev = getPreviousRange(current, period);
        return new SalesComparison(
            sumSales(storeId, current),
            sumSales(storeId, prev)
        );
    }

    // 날짜 계산 로직 분리: "이전 기간"을 자동으로 구해줌
    private DateRange getPreviousRange(DateRange current, SalesPeriod period) {
        LocalDate startDate = current.start().toLocalDate();
        LocalDate endDate = current.end().toLocalDate().minusDays(1);

        return switch (period) {
            case DAY -> DateRangeUtils.between(startDate.minusDays(1), endDate.minusDays(1));
            case WEEK -> DateRangeUtils.between(startDate.minusWeeks(1), endDate.minusWeeks(1));
            case MONTH -> {
                // 한 달 전은 날짜 수(28, 30, 31)가 다르므로 YearMonth 활용이 가장 정확함
                YearMonth currentMonth = YearMonth.from(startDate);
                YearMonth prevMonth = currentMonth.minusMonths(1);
                yield DateRangeUtils.between(prevMonth.atDay(1), prevMonth.atEndOfMonth());
            }
            default -> throw new IllegalArgumentException(ErrorCodes.UNSUPPORTED_PERIOD);
        };
    }
}