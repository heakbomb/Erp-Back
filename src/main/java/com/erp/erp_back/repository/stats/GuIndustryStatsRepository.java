// src/main/java/com/erp/erp_back/repository/stats/GuIndustryStatsRepository.java
package com.erp.erp_back.repository.stats;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class GuIndustryStatsRepository {

    @PersistenceContext
    private EntityManager em;

    /** 1) 구 × 업종 매출 합 */
    public record GuIndustrySalesRow(
            String sigunguCdNm,
            String industry,
            BigDecimal guIndustrySales
    ) {}

    /** 2) 업종별 구당 평균 매출 */
    public record IndustryAvgPerGuRow(
            String industry,
            BigDecimal avgSalesPerGu
    ) {}

    /** 3) 업종별 매장 수 랭킹 */
    public record IndustryStoreCountRow(
            String industry,
            Long storeCount
    ) {}

    /** 4) 중분류별 매장 수 랭킹(매장 기준) */
    public record CategoryStoreCountRow(
            String categoryName,
            Long storeCount
    ) {}

    /** 5) 구 × 중분류 매장 수(매장 기준) */
    public record GuCategoryStoreCountRow(
            String sigunguCdNm,
            String categoryName,
            Long storeCount
    ) {}

          /** ✅ 구별 중분류 판매 수량 TOP */
    public record GuCategoryQtyRow(
            String sigunguCdNm,
            String categoryName,
            Long totalQty
    ) {}

    // =========================================================
    // 1) 구 × 업종 매출 합
    // =========================================================
    public List<GuIndustrySalesRow> fetchGuIndustrySales(LocalDate fromDate, LocalDate toDate) {

        String sql = """
            SELECT
                a.sigungu_cd_nm AS sigunguCdNm,
                s.industry      AS industry,
                SUM(ds.total_sales) AS guIndustrySales
            FROM sales_daily_summary ds
            JOIN store s ON s.store_id = ds.store_id
            JOIN store_trade_area a ON a.store_id = s.store_id
            WHERE ds.summary_date >= :fromDate
              AND ds.summary_date <  :toDate
            GROUP BY a.sigungu_cd_nm, s.industry
            ORDER BY a.sigungu_cd_nm, guIndustrySales DESC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        return rows.stream()
                .map(r -> new GuIndustrySalesRow(
                        (String) r[0],
                        (String) r[1],
                        toBigDecimal(r[2])
                ))
                .toList();
    }

    // =========================================================
    // 2) 업종별 구당 평균 매출
    // =========================================================
    public List<IndustryAvgPerGuRow> fetchIndustryAvgSalesPerGu(LocalDate fromDate, LocalDate toDate) {

        String sql = """
            SELECT x.industry AS industry,
                   AVG(x.gu_industry_sales) AS avgSalesPerGu
            FROM (
              SELECT a.sigungu_cd_nm,
                     s.industry,
                     SUM(ds.total_sales) AS gu_industry_sales
              FROM sales_daily_summary ds
              JOIN store s ON s.store_id = ds.store_id
              JOIN store_trade_area a ON a.store_id = s.store_id
              WHERE ds.summary_date >= :fromDate
                AND ds.summary_date <  :toDate
              GROUP BY a.sigungu_cd_nm, s.industry
            ) x
            GROUP BY x.industry
            ORDER BY avgSalesPerGu DESC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        return rows.stream()
                .map(r -> new IndustryAvgPerGuRow(
                        (String) r[0],
                        toBigDecimal(r[1])
                ))
                .toList();
    }

    // =========================================================
    // 3) 업종별 매장 수 랭킹 (구 매핑된 매장 기준)
    // =========================================================
    public List<IndustryStoreCountRow> fetchIndustryStoreCountRank() {

        String sql = """
            SELECT s.industry AS industry,
                   COUNT(*) AS storeCount
            FROM store s
            JOIN store_trade_area a ON a.store_id = s.store_id
            GROUP BY s.industry
            ORDER BY storeCount DESC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return rows.stream()
                .map(r -> new IndustryStoreCountRow(
                        (String) r[0],
                        toLong(r[1])
                ))
                .toList();
    }

    // =========================================================
    // 4) 중분류별 매장 수 랭킹 (매장 기준)
    // =========================================================
    public List<CategoryStoreCountRow> fetchCategoryStoreCountRank() {

        String sql = """
            SELECT mi.category_name AS categoryName,
                   COUNT(DISTINCT mi.store_id) AS storeCount
            FROM menu_item mi
            GROUP BY mi.category_name
            ORDER BY storeCount DESC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return rows.stream()
                .map(r -> new CategoryStoreCountRow(
                        (String) r[0],
                        toLong(r[1])
                ))
                .toList();
    }

    // =========================================================
    // 5) 구 × 중분류 매장 수 (매장 기준)
    // =========================================================
    public List<GuCategoryStoreCountRow> fetchGuCategoryStoreCount() {

        String sql = """
            SELECT a.sigungu_cd_nm AS sigunguCdNm,
                   mi.category_name AS categoryName,
                   COUNT(DISTINCT mi.store_id) AS storeCount
            FROM store_trade_area a
            JOIN menu_item mi ON mi.store_id = a.store_id
            GROUP BY a.sigungu_cd_nm, mi.category_name
            ORDER BY a.sigungu_cd_nm, storeCount DESC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return rows.stream()
                .map(r -> new GuCategoryStoreCountRow(
                        (String) r[0],
                        (String) r[1],
                        toLong(r[2])
                ))
                .toList();
    }


    /** ✅ 구별 중분류 판매 수량 TOP-N (구 필터 + 기간) */
public List<GuCategoryQtyRow> fetchGuCategoryQtyTop(
            String sigunguCdNm,
            LocalDate fromDate,
            LocalDate toDate,
            int limit
    ) {
        String sql = """
            SELECT
                a.sigungu_cd_nm,
                mi.category_name,
                SUM(smd.total_quantity) AS total_qty
            FROM sales_menu_daily_summary smd
            JOIN menu_item mi ON mi.menu_id = smd.menu_id
            JOIN store_trade_area a ON a.store_id = smd.store_id
            WHERE smd.summary_date >= :fromDate
              AND smd.summary_date <  :toDate
              AND a.sigungu_cd_nm = :sigunguCdNm
            GROUP BY a.sigungu_cd_nm, mi.category_name
            ORDER BY total_qty DESC
            LIMIT %d
        """.formatted(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setParameter("sigunguCdNm", sigunguCdNm)
                .getResultList();

        return rows.stream()
                .map(r -> new GuCategoryQtyRow(
                        (String) r[0],
                        (String) r[1],
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }
    // =========================================================
    // Utils
    // =========================================================
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(v));
    }

    private static Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }
}
