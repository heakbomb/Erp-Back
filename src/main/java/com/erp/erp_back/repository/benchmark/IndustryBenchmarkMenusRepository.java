// src/main/java/com/erp/erp_back/repository/benchmark/IndustryBenchmarkMenusRepository.java
package com.erp.erp_back.repository.benchmark;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndustryBenchmarkMenusRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * ✅ 업종(industry)별 "중분류(category_name)" 랭킹 TOP N
     * - sampleCount(매장 수) 포함
     * - sampleCount >= minSample 조건
     */
    private static final String SQL_CATEGORY_RANK = """
            WITH cat AS (
              SELECT
                st.industry AS industry,
                mi.category_name AS category_name,
                SUM(COALESCE(s.total_quantity,0)) AS qty,
                COUNT(DISTINCT s.store_id) AS sample_count
              FROM sales_menu_daily_summary s
              JOIN menu_item mi ON mi.menu_id = s.menu_id
              JOIN store st ON st.store_id = s.store_id
              WHERE st.industry = ?
                AND s.summary_date BETWEEN ? AND ?
                AND mi.status = 'ACTIVE'
              GROUP BY st.industry, mi.category_name
              HAVING COUNT(DISTINCT s.store_id) >= ?
            ),
            ranked AS (
              SELECT
                industry,
                category_name,
                qty,
                sample_count,
                ROUND(qty * 100.0 / NULLIF(SUM(qty) OVER (PARTITION BY industry), 0), 1) AS share_qty,
                DENSE_RANK() OVER (PARTITION BY industry ORDER BY qty DESC) AS rnk
              FROM cat
            )
            SELECT
              industry,
              category_name,
              qty,
              share_qty,
              rnk,
              sample_count
            FROM ranked
            WHERE rnk <= ?
            ORDER BY rnk
            """;

    /**
     * ✅ 업종 + 중분류에서 "메뉴 TOP3"
     * - sampleCount(매장 수) 포함
     * - sampleCount >= minSample 조건 (업종+중분류 단위로 체크)
     */
    private static final String SQL_TOP3_MENUS_IN_CATEGORY = """
                WITH base AS (
              SELECT
                st.industry AS industry,
                mi.category_name AS category_name,
                mi.menu_name AS menu_name,
                SUM(COALESCE(s.total_quantity,0)) AS qty,
                SUM(COALESCE(s.total_amount,0))   AS amt,
                COUNT(DISTINCT s.store_id) AS sample_count
              FROM sales_menu_daily_summary s
              JOIN menu_item mi ON mi.menu_id = s.menu_id
              JOIN store st ON st.store_id = s.store_id
              WHERE st.industry = ?
                AND mi.category_name = ?
                AND s.summary_date BETWEEN ? AND ?
                AND mi.status = 'ACTIVE'
              GROUP BY st.industry, mi.category_name, mi.menu_name
              HAVING COUNT(DISTINCT s.store_id) >= ?
            ),
            ranked AS (
              SELECT
                industry,
                category_name,
                menu_name,
                qty,
                amt,
                sample_count,
                ROUND(qty * 100.0 / NULLIF(SUM(qty) OVER (PARTITION BY industry, category_name), 0), 1) AS share_qty,
                DENSE_RANK() OVER (PARTITION BY industry, category_name ORDER BY qty DESC) AS rnk
              FROM base
            )
            SELECT
              industry,
              category_name,
              menu_name,
              qty,
              amt,
              share_qty,
              rnk,
              sample_count
            FROM ranked
            WHERE rnk <= 3
            ORDER BY rnk, qty DESC""";

    public List<CategoryRankRow> findCategoryRankTopN(
            String industry, LocalDate from, LocalDate to, int minSample, int topN) {
        return jdbcTemplate.query(
                SQL_CATEGORY_RANK,
                categoryRankMapper(),
                industry, from, to, minSample, topN);
    }

    private RowMapper<CategoryRankRow> categoryRankMapper() {
        return new RowMapper<>() {
            @Override
            public CategoryRankRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new CategoryRankRow(
                        rs.getString("industry"),
                        rs.getString("category_name"),
                        rs.getLong("qty"),
                        rs.getBigDecimal("share_qty"),
                        rs.getInt("rnk"),
                        rs.getInt("sample_count"));
            }
        };
    }

    private RowMapper<CategoryTopMenuRow> topMenuMapper() {
        return new RowMapper<>() {
            @Override
            public CategoryTopMenuRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new CategoryTopMenuRow(
                        rs.getString("industry"),
                        rs.getString("category_name"),
                        rs.getLong("menu_id"),
                        rs.getString("menu_name"),
                        rs.getLong("qty"),
                        rs.getBigDecimal("share_qty"),
                        rs.getBigDecimal("amt"),
                        rs.getInt("rnk"),
                        rs.getInt("sample_count"));
            }
        };
    }

    public record CategoryRankRow(
            String industry,
            String categoryName,
            long quantity,
            BigDecimal shareQty,
            int rank,
            int sampleCount) {
    }

    public record CategoryTopMenuRow(
            String industry,
            String categoryName,
            long menuId,
            String menuName,
            long quantity,
            BigDecimal shareQty,
            BigDecimal amount,
            int rank,
            int sampleCount) {
    }
}
