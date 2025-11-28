package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.SalesTransaction;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

      @Query("SELECT SUM(t.totalAmount) FROM SalesTransaction t " +
           "WHERE t.store.storeId = :storeId " +
           "AND t.transactionTime BETWEEN :from AND :to " +
           "AND t.status = 'PAID'") 
    BigDecimal sumTotalAmountByStoreIdBetween(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

        @Query("SELECT COUNT(t) FROM SalesTransaction t " +
           "WHERE t.store.storeId = :storeId " +
           "AND t.transactionTime BETWEEN :from AND :to " +
           "AND t.status = 'PAID'") 
    Long countByStoreStoreIdAndTransactionTimeBetween(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

        @Query("SELECT new map(FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m-%d') as date, SUM(t.totalAmount) as sales) " +
           "FROM SalesTransaction t " +
           "WHERE t.store.storeId = :storeId " +
           "AND t.transactionTime BETWEEN :from AND :to " +
           "AND t.status = 'PAID' " + 
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionTime, '%Y-%m-%d') " +
           "ORDER BY date ASC")
    List<Map<String, Object>> findDailySalesStats(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

        // 대시보드용: 특정 사업장의 최근 거래 1건
        Optional<SalesTransaction> findTopByStoreStoreIdOrderByTransactionTimeDesc(Long storeId);

        Page<SalesTransaction> findByStoreStoreIdAndTransactionTimeBetween(
                        Long storeId,
                        LocalDateTime start,
                        LocalDateTime end,
                        Pageable pageable);

}