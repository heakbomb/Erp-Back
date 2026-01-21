package com.erp.erp_back.repository.erp;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.IndustryBenchmark;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class IndustryBenchmarkRepository {

    @PersistenceContext
    private EntityManager em;

    public IndustryBenchmark findBestMatch(String sigunguCdNm, String industry, String subCategoryName) {
        String sql = """
            SELECT *
            FROM industry_benchmark b
            WHERE b.industry = :industry
              AND (b.sigungu_cd_nm = :sigungu OR b.sigungu_cd_nm IS NULL)
              AND (b.sub_category_name = :subCat OR b.sub_category_name IS NULL)
            ORDER BY
              (b.sigungu_cd_nm IS NOT NULL) DESC,        -- 구 우선
              (b.sub_category_name IS NOT NULL) DESC,    -- 중분류 우선
              b.updated_at DESC
            LIMIT 1
        """;

        @SuppressWarnings("unchecked")
        List<IndustryBenchmark> rows = em.createNativeQuery(sql, IndustryBenchmark.class)
                .setParameter("industry", industry)
                .setParameter("sigungu", sigunguCdNm)
                .setParameter("subCat", subCategoryName)
                .getResultList();

        return rows.isEmpty() ? null : rows.get(0);
    }
}