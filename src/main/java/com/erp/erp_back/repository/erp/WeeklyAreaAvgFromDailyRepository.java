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

    @Query(value = """
        SELECT t.week_index AS weekIndex,
               COALESCE(AVG(t.store_week_sum), 0) AS areaAvgSales,
               COUNT(*) AS nearStoreCount
        FROM (
            SELECT ds.store_id,
                   (FLOOR((DAY(ds.summary_date)-1)/7) + 1) AS week_index,
                   SUM(ds.total_sales) AS store_week_sum
            FROM sales_daily_summary ds
            JOIN store_neighbor sn
              ON sn.neighbor_store_id = ds.store_id
             AND sn.store_id = :storeId
             AND sn.radius_m = :radiusM
            WHERE ds.summary_date >= :fromDate
              AND ds.summary_date <  :toDate
            GROUP BY ds.store_id, week_index
            HAVING SUM(ds.total_sales) > 0
        ) t
        GROUP BY t.week_index
        ORDER BY t.week_index
        """, nativeQuery = true)
    List<Row> fetchWeeklyAreaAvg(
            @Param("storeId") Long storeId,
            @Param("radiusM") int radiusM,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
