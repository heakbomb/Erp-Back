package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.MonthlySalesReportResponse;
import com.erp.erp_back.dto.erp.MonthlySalesSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.dto.erp.WeeklySalesPointResponse;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;

import static com.erp.erp_back.util.BigDecimalUtils.nz;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesLineItemRepository salesLineItemRepository; // ✅ 추가

    public MonthlySalesReportResponse getMonthlyReport(Long storeId, int year, int month) {

        YearMonth targetMonth = YearMonth.of(year, month);

        MonthlySalesSummaryResponse summary =
                buildSummary(storeId, targetMonth);

        // ✅ 방법 A: 기타 제거, TopN만 (매출액 기준)
        List<TopMenuStatsResponse> topMenus =
                buildTopMenusTopN(storeId, targetMonth, 5);

        List<WeeklySalesPointResponse> weeklySales =
                buildWeeklySalesForMonth(storeId, targetMonth);

        return MonthlySalesReportResponse.builder()
                .summary(summary)
                .topMenus(topMenus)
                .weeklySales(weeklySales)
                .build();
    }

    private MonthlySalesSummaryResponse buildSummary(Long storeId, YearMonth targetMonth) {

        YearMonth lastMonth = targetMonth.minusMonths(1);

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

    private List<WeeklySalesPointResponse> buildWeeklySalesForMonth(Long storeId, YearMonth targetMonth) {

        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to   = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Map<String, Object>> rows =
                salesTransactionRepository.findWeeklySalesStats(storeId, from, to);

        List<WeeklySalesPointResponse> result = new ArrayList<>();

        int weekIndex = 1;
        for (Map<String, Object> row : rows) {
            BigDecimal mySales = nz((BigDecimal) row.get("sales"));
            BigDecimal areaAvg = mySales; // TODO: 실제 상권 평균으로 교체

            result.add(WeeklySalesPointResponse.builder()
                    .weekIndex(weekIndex++)
                    .mySales(mySales)
                    .areaAvgSales(areaAvg)
                    .build());
        }

        return result;
    }

    // ✅ 월간도 /top-menus 와 동일한 로직(라인아이템 집계)으로 TopN만 반환
    private List<TopMenuStatsResponse> buildTopMenusTopN(Long storeId, YearMonth targetMonth, int topN) {

        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to   = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        List<TopMenuStatsResponse> rows =
                salesLineItemRepository.findTopMenuStats(storeId, from, to);

        if (rows == null || rows.isEmpty()) return List.of();

        return rows.stream()
                .limit(topN)
                .toList();
    }
}
