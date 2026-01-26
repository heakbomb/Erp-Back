// src/main/java/com/erp/erp_back/service/erp/IndustryBenchmarkBuildService.java
package com.erp.erp_back.service.erp;

import com.erp.erp_back.repository.erp.IndustryBenchmarkRepository;
import com.erp.erp_back.repository.erp.PayrollCostRepository;
import com.erp.erp_back.repository.erp.ProfitBenchmarkQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndustryBenchmarkBuildService {

    private static final int MIN_SAMPLE_COUNT = 3;

    private final ProfitBenchmarkQueryRepository qRepo;
    private final PayrollCostRepository payrollRepo;
    private final IndustryBenchmarkRepository benchRepo;

    @Transactional
    public void buildMonthly(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.plusMonths(1).atDay(1);

        var scopes = qRepo.findStoreScopes();

        record Metric(String sigungu, String industry, BigDecimal laborRate) {}
        List<Metric> metrics = new ArrayList<>();

        for (var s : scopes) {
            Long storeId = s.storeId();
            String sigungu = s.sigunguCdNm();
            String industry = s.industry();

            BigDecimal sales = qRepo.sumMonthlySales(storeId, from, to);
            if (sales == null || sales.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal labor = payrollRepo.sumMonthlyLabor(storeId, yearMonth);
            if (labor == null) labor = BigDecimal.ZERO;

            BigDecimal laborRate = labor.divide(sales, 6, RoundingMode.HALF_UP); // 0~1
            metrics.add(new Metric(sigungu, industry, laborRate));
        }

        Map<String, List<Metric>> grouped =
                metrics.stream().collect(Collectors.groupingBy(m -> m.sigungu() + "||" + m.industry()));

        for (var e : grouped.entrySet()) {
            List<Metric> list = e.getValue();
            if (list.size() < MIN_SAMPLE_COUNT) continue;

            String[] p = e.getKey().split("\\|\\|");
            String sigungu = p[0];
            String industry = p[1];

            BigDecimal avgLabor = avg(list.stream().map(Metric::laborRate).toList(), 4);
            BigDecimal avgCogs = defaultCogsRate(industry).setScale(4, RoundingMode.HALF_UP);

            benchRepo.upsertMonthly(
                    yearMonth,
                    sigungu,
                    industry,
                    "ALL",
                    avgCogs,
                    avgLabor,
                    list.size()
            );
        }
    }

    private BigDecimal avg(List<BigDecimal> values, int scale) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultCogsRate(String industry) {
        if (industry == null) return new BigDecimal("0.33");
        return switch (industry) {
            case "KOREAN"  -> new BigDecimal("0.35");
            case "CHICKEN" -> new BigDecimal("0.40");
            default        -> new BigDecimal("0.33");
        };
    }
}
