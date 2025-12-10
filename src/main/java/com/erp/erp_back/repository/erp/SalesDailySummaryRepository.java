package com.erp.erp_back.repository.erp;

import com.erp.erp_back.dto.erp.DashboardStatsProjection;
import com.erp.erp_back.entity.erp.SalesDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface SalesDailySummaryRepository extends JpaRepository<SalesDailySummary, Long> {

    /**
     * [최적화 쿼리]
     * 과거 데이터(어제까지)의 모든 통계(어제, 이번주누적, 저번주, 이번달누적, 저번달)를 
     * CASE WHEN 구문으로 단 1회 스캔하여 가져옵니다.
     */
    @Query("""
        SELECT new com.erp.erp_back.dto.erp.DashboardStatsProjection(
            SUM(CASE WHEN s.summaryDate = :yesterday THEN s.totalSales ELSE 0 END),
            SUM(CASE WHEN s.summaryDate BETWEEN :weekStart AND :yesterday THEN s.totalSales ELSE 0 END),
            SUM(CASE WHEN s.summaryDate BETWEEN :prevWeekStart AND :prevWeekEnd THEN s.totalSales ELSE 0 END),
            SUM(CASE WHEN s.summaryDate BETWEEN :monthStart AND :yesterday THEN s.totalSales ELSE 0 END),
            SUM(CASE WHEN s.summaryDate BETWEEN :prevMonthStart AND :prevMonthEnd THEN s.totalSales ELSE 0 END)
        )
        FROM SalesDailySummary s
        WHERE s.storeId = :storeId
          AND s.summaryDate >= :minDate
    """)
    DashboardStatsProjection findIntegratedStats(
        @Param("storeId") Long storeId,
        @Param("yesterday") LocalDate yesterday,
        @Param("weekStart") LocalDate weekStart,
        @Param("prevWeekStart") LocalDate prevWeekStart,
        @Param("prevWeekEnd") LocalDate prevWeekEnd,
        @Param("monthStart") LocalDate monthStart,
        @Param("prevMonthStart") LocalDate prevMonthStart,
        @Param("prevMonthEnd") LocalDate prevMonthEnd,
        @Param("minDate") LocalDate minDate
    );

    @Query("select max(s.summaryDate) from SalesDailySummary s")
    LocalDate findMaxSummaryDate();
}