// src/main/java/com/erp/erp_back/controller/admin/AdminStatsController.java
package com.erp.erp_back.controller.admin;

import com.erp.erp_back.repository.stats.GuIndustryStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final GuIndustryStatsRepository statsRepo;

    // 1) 구 × 업종 매출 합
    @GetMapping("/gu-industry-sales")
    public List<GuIndustryStatsRepository.GuIndustrySalesRow> guIndustrySales(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        return statsRepo.fetchGuIndustrySales(from, to);
    }

    // 2) 업종별 구당 평균 매출
    @GetMapping("/industry-avg-sales-per-gu")
    public List<GuIndustryStatsRepository.IndustryAvgPerGuRow> industryAvgSalesPerGu(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        return statsRepo.fetchIndustryAvgSalesPerGu(from, to);
    }

    // 3) 업종별 매장 수 랭킹
    @GetMapping("/industry-store-count-rank")
    public List<GuIndustryStatsRepository.IndustryStoreCountRow> industryStoreCountRank() {
        return statsRepo.fetchIndustryStoreCountRank();
    }

    // 4) 중분류별 매장 수 랭킹 (매장 기준)
    @GetMapping("/category-store-count-rank")
    public List<GuIndustryStatsRepository.CategoryStoreCountRow> categoryStoreCountRank() {
        return statsRepo.fetchCategoryStoreCountRank();
    }

    // 5) 구 × 중분류 매장 수 (매장 기준)
    @GetMapping("/gu-category-store-count")
    public List<GuIndustryStatsRepository.GuCategoryStoreCountRow> guCategoryStoreCount() {
        return statsRepo.fetchGuCategoryStoreCount();
    }

    /**
     * ✅ 구별 중분류 판매 수량 TOP-N
     * - sigunguCdNm 없으면: 전체 구 기준으로 (구별+카테고리) 집계 결과가 나오지만,
     * "강남구 TOP10" 같은 목적이면 sigunguCdNm을 꼭 넣는 걸 추천.
     */
    @GetMapping("/gu-category-qty-top")
    public List<GuIndustryStatsRepository.GuCategoryQtyRow> guCategoryQtyTop(
            @RequestParam(required = false) String sigunguCdNm,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        return statsRepo.fetchGuCategoryQtyTop(sigunguCdNm, from, to, limit);
    }
}
