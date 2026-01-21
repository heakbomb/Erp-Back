package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

public record ProfitBenchmarkResponse(
        Long storeId,
        int year,
        int month,

        String sigunguCdNm,
        String industry,
        String repSubCategoryName,     // 대표 중분류(없으면 null)

        BigDecimal sales,              // 매출(기간 합)

        BigDecimal myCogsRate,         // 내 원가율
        BigDecimal myLaborRate,        // 내 인건비율
        BigDecimal myCogsAmount,       // 내 원가금액
        BigDecimal myLaborAmount,      // 내 인건비금액
        BigDecimal myProfit,           // 내 순이익(추정)
        BigDecimal myProfitRate,       // 내 순이익률(추정)

        BigDecimal benchCogsRate,      // 업계 평균 원가율
        BigDecimal benchLaborRate,     // 업계 평균 인건비율
        BigDecimal benchCogsAmount,    // 업계 평균 환산 원가금액(내 매출 기준)
        BigDecimal benchLaborAmount,   // 업계 평균 환산 인건비금액
        BigDecimal benchProfit,        // 업계 평균 환산 순이익
        BigDecimal benchProfitRate,    // 업계 평균 환산 순이익률

        BigDecimal diffProfit,         // (내 - 평균) 순이익 차이
        BigDecimal diffProfitRatePct,  // (내 - 평균) 순이익률 차이(%p)

        String basis,                  // "CONFIG" (지금은 이걸로 고정)
        Integer sampleCount            // industry_benchmark.sample_count
) {}
