package com.erp.erp_back.service.benchmark;

import com.erp.erp_back.dto.benchmark.IndustrySubCategoryRankResponse;
import com.erp.erp_back.repository.benchmark.IndustrySubCategoryBenchmarkRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndustrySubCategoryBenchmarkService {

  private final IndustrySubCategoryBenchmarkRepository repository;

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final int DEFAULT_PERIOD_DAYS = 30;
  private static final int DEFAULT_TOP = 5;

  // ✅ 운영 기준 (요구사항): 표본 5개 이상일 때만 공개
  private static final int MIN_SAMPLE_COUNT = 5;

  public List<IndustrySubCategoryRankResponse> getSubCategoryRankTopN(
      String industry,
      String categoryName,
      Integer periodDays,
      Integer top
  ) {
    int days = (periodDays == null || periodDays <= 0) ? DEFAULT_PERIOD_DAYS : periodDays;
    int topN = (top == null || top <= 0) ? DEFAULT_TOP : top;

    LocalDate to = LocalDate.now(KST);
    LocalDate from = to.minusDays(days - 1L);

    var rows = repository.findSubCategoryRankTopN(
        industry,
        categoryName,
        from,
        to,
        MIN_SAMPLE_COUNT,
        topN
    );

    List<IndustrySubCategoryRankResponse> res = new ArrayList<>();
    for (var r : rows) {
      res.add(new IndustrySubCategoryRankResponse(
          r.industry(),
          r.categoryName(),
          r.subCategoryName(),
          r.quantity(),
          r.shareQty(),
          r.rank(),
          r.sampleCount()
      ));
    }
    return res;
  }
}
