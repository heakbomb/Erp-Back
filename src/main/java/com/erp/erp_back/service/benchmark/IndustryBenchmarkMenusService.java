// src/main/java/com/erp/erp_back/service/benchmark/IndustryBenchmarkMenusService.java
package com.erp.erp_back.service.benchmark;

import com.erp.erp_back.dto.benchmark.IndustryCategoryRankResponse;
import com.erp.erp_back.repository.benchmark.IndustryBenchmarkMenusRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndustryBenchmarkMenusService {

  private final IndustryBenchmarkMenusRepository repository;

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final int DEFAULT_PERIOD_DAYS = 30;
  private static final int DEFAULT_TOP_CATEGORIES = 10;

  // ✅ 요구사항: 벤치마킹은 5개 이상 사업장 집계만 공개
  private static final int MIN_SAMPLE_COUNT = 5; // 필요하면 설정으로 빼도 됨

  public List<IndustryCategoryRankResponse> getCategoryRankTopN(String industry, Integer periodDays, Integer top) {
    int days = (periodDays == null || periodDays <= 0) ? DEFAULT_PERIOD_DAYS : periodDays;
    int topN = (top == null || top <= 0) ? DEFAULT_TOP_CATEGORIES : top;

    LocalDate to = LocalDate.now(KST);
    LocalDate from = to.minusDays(days - 1L);

    var rows = repository.findCategoryRankTopN(industry, from, to, MIN_SAMPLE_COUNT, topN);

    List<IndustryCategoryRankResponse> res = new ArrayList<>();
    for (var r : rows) {
      res.add(new IndustryCategoryRankResponse(
          r.industry(),
          r.categoryName(),
          r.quantity(),
          r.shareQty(),
          r.rank(),
          r.sampleCount()
      ));
    }
    return res;
  }

}
