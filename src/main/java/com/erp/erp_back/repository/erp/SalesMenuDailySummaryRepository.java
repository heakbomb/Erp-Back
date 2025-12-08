package com.erp.erp_back.repository.erp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.dto.erp.TopMenuStatsResponse; // DTO 재활용
import com.erp.erp_back.entity.erp.SalesMenuDailySummary;

public interface SalesMenuDailySummaryRepository extends JpaRepository<SalesMenuDailySummary, Long> {

    // 요약된 테이블에서 조회하므로 속도가 매우 빠릅니다.
    @Query("SELECT new com.erp.erp_back.dto.erp.TopMenuStatsResponse(" +
           "  s.menuItem.menuId, s.menuItem.menuName, SUM(s.totalQuantity), SUM(s.totalAmount)) " +
           "FROM SalesMenuDailySummary s " +
           "WHERE s.store.storeId = :storeId " +
           "AND s.summaryDate BETWEEN :from AND :to " +
           "GROUP BY s.menuItem.menuId, s.menuItem.menuName " +
           "ORDER BY SUM(s.totalAmount) DESC")
    List<TopMenuStatsResponse> findTopMenuStats(
            @Param("storeId") Long storeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}