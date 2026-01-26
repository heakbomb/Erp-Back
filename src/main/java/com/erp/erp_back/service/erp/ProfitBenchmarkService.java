// src/main/java/com/erp/erp_back/service/stats/ProfitBenchmarkService.java
package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.erp.erp_back.dto.erp.ProfitBenchmarkResponse;
import com.erp.erp_back.entity.erp.IndustryBenchmark;
import com.erp.erp_back.repository.erp.IndustryBenchmarkRepository;
import com.erp.erp_back.repository.erp.PayrollCostRepository;
import com.erp.erp_back.repository.erp.ProfitBenchmarkQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfitBenchmarkService {

        private final ProfitBenchmarkQueryRepository qRepo;
        private final IndustryBenchmarkRepository benchRepo;
        private final PayrollCostRepository payrollRepo;

        public ProfitBenchmarkResponse getMonthlyProfitBenchmark(
                        Long storeId,
                        int year,
                        int month,
                        BigDecimal myCogsRate // ✅ 원가율만 입력
        ) {
                int y = Math.max(2000, Math.min(year, 2100));
                int m = Math.max(1, Math.min(month, 12));

                myCogsRate = clamp01(myCogsRate);

                YearMonth ym = YearMonth.of(y, m);
                LocalDate from = ym.atDay(1);
                LocalDate to = ym.plusMonths(1).atDay(1);
                String payrollMonth = ym.toString(); // "YYYY-MM"

                // 기본 정보
                String sigungu = qRepo.findSigunguCdNm(storeId);
                String industry = qRepo.findIndustry(storeId);
                String repSubCat = qRepo.findTopSubCategoryByQty(storeId, from, to);

                BigDecimal sales = qRepo.sumMonthlySales(storeId, from, to);

                // ✅ 월 인건비 자동 계산
                BigDecimal myLaborAmount = payrollRepo.sumMonthlyLabor(storeId, payrollMonth);

                BigDecimal myLaborRate = rate(myLaborAmount, sales);

                // 내 비용/이익
                BigDecimal myCogsAmount = sales.multiply(myCogsRate);
                BigDecimal myProfit = sales
                                .subtract(myCogsAmount)
                                .subtract(myLaborAmount);

                BigDecimal myProfitRate = rate(myProfit, sales);

                // 업계 평균(설정값)
                IndustryBenchmark bench = benchRepo.findBestMatch(payrollMonth, sigungu, industry, repSubCat);

                if (bench == null) {
                        bench = benchRepo.findBestMatch(payrollMonth, null, industry, null);
                }

                BigDecimal benchCogsRate = bench != null ? bench.getAvgCogsRate() : BigDecimal.ZERO;
                BigDecimal benchLaborRate = bench != null ? bench.getAvgLaborRate() : BigDecimal.ZERO;
                Integer sampleCount = bench != null ? bench.getSampleCount() : 0;

                BigDecimal benchCogsAmount = sales.multiply(benchCogsRate);
                BigDecimal benchLaborAmount = sales.multiply(benchLaborRate);
                BigDecimal benchProfit = sales.subtract(benchCogsAmount).subtract(benchLaborAmount);
                BigDecimal benchProfitRate = rate(benchProfit, sales);

                BigDecimal diffProfit = myProfit.subtract(benchProfit);
                BigDecimal diffProfitRatePct = myProfitRate.subtract(benchProfitRate)
                                .multiply(BigDecimal.valueOf(100));

                return new ProfitBenchmarkResponse(
                                storeId, y, m,
                                sigungu, industry, repSubCat,
                                money(sales),

                                scale4(myCogsRate),
                                scale4(myLaborRate),
                                money(myCogsAmount),
                                money(myLaborAmount),
                                money(myProfit),
                                pct4(myProfitRate),

                                scale4(benchCogsRate),
                                scale4(benchLaborRate),
                                money(benchCogsAmount),
                                money(benchLaborAmount),
                                money(benchProfit),
                                pct4(benchProfitRate),

                                money(diffProfit),
                                diffProfitRatePct.setScale(2, RoundingMode.HALF_UP),

                                "CONFIG",
                                sampleCount);
        }

        /* ---------- utils ---------- */

        private static BigDecimal clamp01(BigDecimal v) {
                if (v == null)
                        return BigDecimal.ZERO;
                if (v.compareTo(BigDecimal.ZERO) < 0)
                        return BigDecimal.ZERO;
                if (v.compareTo(BigDecimal.ONE) > 0)
                        return BigDecimal.ONE;
                return v;
        }

        private static BigDecimal rate(BigDecimal num, BigDecimal den) {
                if (den == null || den.compareTo(BigDecimal.ZERO) == 0)
                        return BigDecimal.ZERO;
                return num.divide(den, 6, RoundingMode.HALF_UP);
        }

        private static BigDecimal money(BigDecimal v) {
                return v == null ? BigDecimal.ZERO.setScale(2) : v.setScale(2, RoundingMode.HALF_UP);
        }

        private static BigDecimal pct4(BigDecimal v) {
                return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
        }

        private static BigDecimal scale4(BigDecimal v) {
                return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
        }
}
