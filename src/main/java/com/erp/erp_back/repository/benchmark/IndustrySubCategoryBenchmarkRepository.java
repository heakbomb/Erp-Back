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
public class IndustrySubCategoryBenchmarkRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final String SQL_SUBCATEGORY_RANK = """
    WITH base AS (
      SELECT
        st.industry AS industry,
        mi.category_name AS category_name,
        mi.sub_category_name AS sub_category_name,
        SUM(COALESCE(s.total_quantity,0)) AS qty,
        COUNT(DISTINCT s.store_id) AS sample_count
      FROM sales_menu_daily_summary s
      JOIN menu_item mi ON mi.menu_id = s.menu_id
      JOIN store st ON st.store_id = s.store_id
      WHERE st.industry = ?
        AND mi.category_name = ?
        AND s.summary_date BETWEEN ? AND ?
        AND mi.status = 'ACTIVE'
      GROUP BY st.industry, mi.category_name, mi.sub_category_name
      HAVING COUNT(DISTINCT s.store_id) >= ?
    ),
    ranked AS (
      SELECT
        industry,
        category_name,
        sub_category_name,
        qty,
        sample_count,
        ROUND(
          qty * 100.0 / NULLIF(SUM(qty) OVER (PARTITION BY industry, category_name), 0),
          1
        ) AS share_qty,
        DENSE_RANK() OVER (PARTITION BY industry, category_name ORDER BY qty DESC) AS rnk
      FROM base
    )
    SELECT
      industry,
      category_name,
      sub_category_name,
      qty,
      share_qty,
      rnk,
      sample_count
    FROM ranked
    WHERE rnk <= ?
    ORDER BY rnk, qty DESC
    """;

  public List<SubCategoryRankRow> findSubCategoryRankTopN(
      String industry,
      String categoryName,
      LocalDate from,
      LocalDate to,
      int minSample,
      int topN
  ) {
    return jdbcTemplate.query(
        SQL_SUBCATEGORY_RANK,
        subCategoryRankMapper(),
        industry,
        categoryName,
        from,
        to,
        minSample,
        topN
    );
  }

  private RowMapper<SubCategoryRankRow> subCategoryRankMapper() {
    return new RowMapper<>() {
      @Override
      public SubCategoryRankRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SubCategoryRankRow(
            rs.getString("industry"),
            rs.getString("category_name"),
            rs.getString("sub_category_name"),
            rs.getLong("qty"),
            rs.getBigDecimal("share_qty"),
            rs.getInt("rnk"),
            rs.getInt("sample_count")
        );
      }
    };
  }

  public record SubCategoryRankRow(
      String industry,
      String categoryName,
      String subCategoryName,
      long quantity,
      BigDecimal shareQty,
      int rank,
      int sampleCount
  ) {}
}
