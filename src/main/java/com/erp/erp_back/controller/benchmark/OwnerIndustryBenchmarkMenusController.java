package com.erp.erp_back.controller.benchmark;

import com.erp.erp_back.dto.benchmark.IndustryCategoryRankResponse;
import com.erp.erp_back.dto.benchmark.IndustrySubCategoryRankResponse;
import com.erp.erp_back.service.benchmark.IndustryBenchmarkMenusService;
import com.erp.erp_back.service.benchmark.IndustrySubCategoryBenchmarkService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/benchmark/industry")
public class OwnerIndustryBenchmarkMenusController {

  private final IndustryBenchmarkMenusService service;
    private final IndustrySubCategoryBenchmarkService industrySubCategoryBenchmarkService;

  /**
   * ✅ 업종 선택 → 중분류 랭킹 TOP N
   * 예) /owner/benchmark/industry/category-rank?industry=CHICKEN&periodDays=30&top=10
   */
  @GetMapping("/category-rank")
  public List<IndustryCategoryRankResponse> categoryRank(
      @RequestParam String industry,
      @RequestParam(required = false, defaultValue = "30") Integer periodDays,
      @RequestParam(required = false, defaultValue = "10") Integer top
  ) {
    // store.industry enum 값과 동일 문자열(예: KOREAN, CHICKEN)로 보내면 됨
    return service.getCategoryRankTopN(industry, periodDays, top);
  }

   /**
   * ✅ 업종 + 중분류 → 소분류 랭킹 TOP N
   *
   * 예)
   * /owner/benchmark/industry/subcategory-rank
   *   ?industry=KOREAN
   *   &categoryName=밥/정식
   *   &periodDays=30
   *   &top=5
   */
  @GetMapping("/subcategory-rank")
  public List<IndustrySubCategoryRankResponse> subCategoryRank(
      @RequestParam String industry,
      @RequestParam String categoryName,
      @RequestParam(required = false, defaultValue = "30") Integer periodDays,
      @RequestParam(required = false, defaultValue = "5") Integer top
  ) {
    return industrySubCategoryBenchmarkService.getSubCategoryRankTopN(industry, categoryName, periodDays, top);
  }
}
