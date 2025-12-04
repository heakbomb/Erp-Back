package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.MonthlySalesReportResponse;
import com.erp.erp_back.dto.erp.MonthlySalesSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuShareResponse;
import com.erp.erp_back.dto.erp.WeeklySalesPointResponse;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;

import static com.erp.erp_back.util.BigDecimalUtils.nz;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportService {

    private final SalesTransactionRepository salesTransactionRepository;

    /**
     * 월간 매출 리포트 메인 진입점
     *
     * 예: 2025년 6월 리포트
     */
    public MonthlySalesReportResponse getMonthlyReport(Long storeId, int year, int month) {

        YearMonth targetMonth = YearMonth.of(year, month);

        MonthlySalesSummaryResponse summary =
                buildSummary(storeId, targetMonth);             // 지난달 / 이번달 / 증감액

        List<TopMenuShareResponse> topMenus =
                buildTopMenus(storeId, targetMonth, 4);         // 상위 4개 + 기타

        List<WeeklySalesPointResponse> weeklySales =
                buildWeeklySalesForMonth(storeId, targetMonth); // 해당 달의 주간 매출

        return MonthlySalesReportResponse.builder()
                .summary(summary)
                .topMenus(topMenus)
                .weeklySales(weeklySales)
                .build();
    }

    // -----------------------------------------------------
    // 1) 요약 정보 (지난달 / 이번달 / 증감액)
    // -----------------------------------------------------
    private MonthlySalesSummaryResponse buildSummary(Long storeId, YearMonth targetMonth) {

        YearMonth lastMonth = targetMonth.minusMonths(1);

        // BETWEEN :from AND :to (포함)이므로, to는 해당 월의 마지막날 23:59:59로 설정
        LocalDateTime thisFrom = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime thisTo   = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        LocalDateTime lastFrom = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastTo   = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal thisTotal = nz(
                salesTransactionRepository.sumTotalAmountByStoreIdBetween(storeId, thisFrom, thisTo)
        );
        BigDecimal lastTotal = nz(
                salesTransactionRepository.sumTotalAmountByStoreIdBetween(storeId, lastFrom, lastTo)
        );

        BigDecimal diff = thisTotal.subtract(lastTotal);

        return MonthlySalesSummaryResponse.builder()
                .lastMonthTotal(lastTotal)
                .thisMonthTotal(thisTotal)
                .diff(diff)
                .build();
    }

    // -----------------------------------------------------
    // 2) 해당 달의 주간 매출 (그래프 + 표)
    // -----------------------------------------------------
    private List<WeeklySalesPointResponse> buildWeeklySalesForMonth(Long storeId, YearMonth targetMonth) {

        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to   = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Map<String, Object>> rows =
                salesTransactionRepository.findWeeklySalesStats(storeId, from, to);

        List<WeeklySalesPointResponse> result = new ArrayList<>();

        int weekIndex = 1; // 1주차, 2주차, ... 라벨용
        for (Map<String, Object> row : rows) {
            BigDecimal mySales = nz((BigDecimal) row.get("sales"));

            // TODO: 나중에 진짜 상권 평균 매출 로직 들어오면 여기에서 areaAvgSales 계산
            BigDecimal areaAvg = mySales; // 현재는 내 매출 = 상권 평균 placeholder

            result.add(WeeklySalesPointResponse.builder()
                    .weekIndex(weekIndex++)
                    .mySales(mySales)
                    .areaAvgSales(areaAvg)
                    .build());
        }

        return result;
    }

    // -----------------------------------------------------
    // 3) 인기 메뉴 비율 (파이 차트)
    // -----------------------------------------------------
    private List<TopMenuShareResponse> buildTopMenus(
            Long storeId,
            YearMonth targetMonth,
            int topN
    ) {
        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to   = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Map<String, Object>> rows =
                salesTransactionRepository.findMenuSalesComposition(storeId, from, to);

        if (rows.isEmpty()) {
            return List.of();
        }

        BigDecimal total = rows.stream()
                .map(m -> (BigDecimal) m.get("sales"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopMenuShareResponse> result = new ArrayList<>();

        // 상위 N개
        int limit = Math.min(topN, rows.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> r = rows.get(i);

            String menuName = (String) r.get("menuName");
            BigDecimal sales = (BigDecimal) r.get("sales");
            BigDecimal rate  = calcRatePercent(sales, total);

            result.add(TopMenuShareResponse.builder()
                    .menuName(menuName)
                    .sales(sales)
                    .rate(rate)
                    .build());
        }

        // 나머지는 "기타"로 합산
        if (rows.size() > limit) {
            BigDecimal others = BigDecimal.ZERO;
            for (int i = limit; i < rows.size(); i++) {
                others = others.add((BigDecimal) rows.get(i).get("sales"));
            }
            BigDecimal rate = calcRatePercent(others, total);

            result.add(TopMenuShareResponse.builder()
                    .menuName("기타")
                    .sales(others)
                    .rate(rate)
                    .build());
        }

        return result;
    }

    private BigDecimal calcRatePercent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP);
    }
}
