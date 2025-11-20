package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.SalesTransaction;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    @Query("SELECT COALESCE(SUM(st.totalAmount), 0) FROM SalesTransaction st " +
           "WHERE st.store.storeId = :storeId " + 
           "AND st.transactionTime >= :start AND st.transactionTime < :end")
    BigDecimal sumSalesAmountByStoreIdBetween( // 메서드 이름은 유지해도 되지만, 헷갈린다면 sumTotalAmount...로 변경 고려
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    @Query("SELECT new map(" +
           "   function('date_format', st.transactionTime, '%Y-%m-%d') as date, " +
           "   SUM(st.totalAmount) as sales" +
           ") " +
           "FROM SalesTransaction st " +
           "WHERE st.store.storeId = :storeId " +
           "  AND st.transactionTime BETWEEN :startDate AND :endDate " +
           "GROUP BY function('date_format', st.transactionTime, '%Y-%m-%d') " +
           "ORDER BY function('date_format', st.transactionTime, '%Y-%m-%d') ASC")
    List<Map<String, Object>> findDailySalesStats(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(st.totalAmount), 0) FROM SalesTransaction st " +
           "WHERE st.transactionTime >= :start AND st.transactionTime < :end")
    BigDecimal sumSalesAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 대시보드용: 특정 사업장의 최근 거래 1건
    Optional<SalesTransaction> findTopByStoreStoreIdOrderByTransactionTimeDesc(Long storeId);
}