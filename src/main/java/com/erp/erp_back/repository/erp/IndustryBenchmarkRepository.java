// src/main/java/com/erp/erp_back/repository/erp/IndustryBenchmarkRepository.java
package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.IndustryBenchmark;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class IndustryBenchmarkRepository {

  @PersistenceContext
  private EntityManager em;

  /**
   * 월별(YYYY-MM) 기준 벤치마크 조회
   *
   * 우선순위:
   * 1) ym + sigungu + industry + subCat
   * 2) ym + sigungu + industry + ALL
   * 3) ym + null(sigungu) + industry + ALL
   */
  public IndustryBenchmark findBestMatch(String yearMonth, String sigunguCdNm, String industry,
      String subCategoryName) {
    String sql = """
            SELECT *
            FROM industry_benchmark b
            WHERE b.year_month = :ym
              AND b.industry = :industry
              AND (:sigungu IS NULL OR b.sigungu_cd_nm = :sigungu OR b.sigungu_cd_nm IS NULL)
              AND (
                    (:subCat IS NOT NULL AND b.sub_category_name = :subCat)
                    OR b.sub_category_name = 'ALL'
                  )
            ORDER BY
              CASE
                WHEN (:sigungu IS NOT NULL AND b.sigungu_cd_nm = :sigungu) THEN 0
                WHEN (b.sigungu_cd_nm IS NULL) THEN 2
                ELSE 1
              END,
              CASE
                WHEN (:subCat IS NOT NULL AND b.sub_category_name = :subCat) THEN 0
                WHEN (b.sub_category_name = 'ALL') THEN 1
                ELSE 2
              END,
              b.updated_at DESC
            LIMIT 1
        """;

    @SuppressWarnings("unchecked")
    List<IndustryBenchmark> rows = em.createNativeQuery(sql, IndustryBenchmark.class)
        .setParameter("ym", yearMonth)
        .setParameter("industry", industry)
        .setParameter("sigungu", sigunguCdNm)
        .setParameter("subCat", subCategoryName)
        .getResultList();

    return rows.isEmpty() ? null : rows.get(0);
  }

  /**
   * 월별 벤치마크 업서트
   * - subCategoryName은 "ALL" 고정 추천
   * - 유니크키: (year_month, sigungu_cd_nm, industry, sub_category_name)
   */
  @Transactional
  public int upsertMonthly(
      String yearMonth,
      String sigunguCdNm,
      String industry,
      String subCategoryName,
      BigDecimal avgCogsRate,
      BigDecimal avgLaborRate,
      int sampleCount) {
    String sql = """
            INSERT INTO `industry_benchmark`
              (`year_month`, `sigungu_cd_nm`, `industry`, `sub_category_name`,
               `avg_cogs_rate`, `avg_labor_rate`, `sample_count`, `updated_at`)
            VALUES
              (?, ?, ?, ?,
               ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
              `avg_cogs_rate`  = ?,
              `avg_labor_rate` = ?,
              `sample_count`   = ?,
              `updated_at`     = NOW()
        """;

    return em.createNativeQuery(sql)
        .setParameter(1, yearMonth)
        .setParameter(2, sigunguCdNm)
        .setParameter(3, industry)
        .setParameter(4, subCategoryName)
        .setParameter(5, avgCogsRate)
        .setParameter(6, avgLaborRate)
        .setParameter(7, sampleCount)
        // UPDATE에 들어갈 값들 (VALUES() 안 쓰고 직접 바인딩)
        .setParameter(8, avgCogsRate)
        .setParameter(9, avgLaborRate)
        .setParameter(10, sampleCount)
        .executeUpdate();
  }
}
