package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.SalesDailySummary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WeeklyAreaAvgFromDailyRepository extends Repository<SalesDailySummary, Long> {

    interface Row {
        Integer getWeekIndex();
        BigDecimal getAreaAvgSales();
        Integer getNearStoreCount();
    }

    /**
     * ✅ 같은 구(sigungu_cd_nm) + 동종 업종(industry) 기준
     * 주차별 "매장 주간합의 평균"을 구함
     *
     * - 비교군: (same sigungu) AND (same industry) AND (store_id != :storeId)
     * - week_index: 월 기준 1~6주 (DAY(summary_date) 기반)
     */
    @Query(value = """
        SELECT t.week_index AS weekIndex,
               COALESCE(AVG(t.store_week_sum), 0) AS areaAvgSales,
               COUNT(*) AS nearStoreCount
        FROM (
            SELECT ds.store_id,
                   (FLOOR((DAY(ds.summary_date)-1)/7) + 1) AS week_index,
                   SUM(ds.total_sales) AS store_week_sum
            FROM sales_daily_summary ds
            JOIN store s2
              ON s2.store_id = ds.store_id
            JOIN store_trade_area a2
              ON a2.store_id = s2.store_id
            WHERE ds.summary_date >= :fromDate
              AND ds.summary_date <  :toDate
              AND s2.store_id <> :storeId
              AND s2.industry = (SELECT industry FROM store WHERE store_id = :storeId)
              AND a2.sigungu_cd_nm = (SELECT sigungu_cd_nm FROM store_trade_area WHERE store_id = :storeId)
            GROUP BY ds.store_id, week_index
            HAVING SUM(ds.total_sales) > 0
        ) t
        GROUP BY t.week_index
        ORDER BY t.week_index
        """, nativeQuery = true)
    List<Row> fetchWeeklyAreaAvg(
            @Param("storeId") Long storeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
