package com.erp.erp_back.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ProfitBenchmarkQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /** ✅ 벤치마크 집계용: (storeId, sigungu, industry) 한 번에 가져오기 */
    public List<StoreScopeRow> findStoreScopes() {
        String sql = """
            SELECT
                s.store_id AS storeId,
                sta.sigungu_cd_nm AS sigunguCdNm,
                s.industry AS industry
            FROM store s
            JOIN store_trade_area sta ON sta.store_id = s.store_id
            WHERE s.status = 'APPROVED'
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return rows.stream()
                .map(r -> new StoreScopeRow(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[2]
                ))
                .toList();
    }

    /** store_trade_area에서 구 이름 */
    public String findSigunguCdNm(Long storeId) {
        String sql = """
            SELECT a.sigungu_cd_nm
            FROM store_trade_area a
            WHERE a.store_id = :storeId
            LIMIT 1
        """;
        @SuppressWarnings("unchecked")
        List<String> rows = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** store 테이블에서 업종(StoreIndustry 문자열) */
    public String findIndustry(Long storeId) {
        String sql = """
            SELECT s.industry
            FROM store s
            WHERE s.store_id = :storeId
        """;
        @SuppressWarnings("unchecked")
        List<String> rows = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 월 매출 합: sales_daily_summary */
    public BigDecimal sumMonthlySales(Long storeId, LocalDate from, LocalDate toExclusive) {
        String sql = """
            SELECT COALESCE(SUM(ds.total_sales), 0)
            FROM sales_daily_summary ds
            WHERE ds.store_id = :storeId
              AND ds.summary_date >= :fromDate
              AND ds.summary_date <  :toDate
        """;
        Object v = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("fromDate", from)
                .setParameter("toDate", toExclusive)
                .getSingleResult();

        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(v));
    }

    /** 월 판매량 기준 top sub_category_name */
    public String findTopSubCategoryByQty(Long storeId, LocalDate from, LocalDate toExclusive) {
        String sql = """
            SELECT mi.sub_category_name
            FROM sales_menu_daily_summary smd
            JOIN menu_item mi ON mi.menu_id = smd.menu_id
            WHERE smd.store_id = :storeId
              AND smd.summary_date >= :fromDate
              AND smd.summary_date <  :toDate
            GROUP BY mi.sub_category_name
            ORDER BY SUM(smd.total_quantity) DESC
            LIMIT 1
        """;

        @SuppressWarnings("unchecked")
        List<String> rows = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("fromDate", from)
                .setParameter("toDate", toExclusive)
                .getResultList();

        return rows.isEmpty() ? null : rows.get(0);
    }

    /** ✅ 내부 DTO */
    public record StoreScopeRow(Long storeId, String sigunguCdNm, String industry) {}
}
