package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.SalesTransaction;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
    
    /** (Admin) 특정 기간 동안의 총 매출액 합계 */
    @Query("SELECT COALESCE(SUM(st.salesAmount), 0) FROM SalesTransaction st " +
           "WHERE st.transactionTime >= :start AND st.transactionTime < :end")
    BigDecimal sumSalesAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /** (신규) 대시보드용: 특정 '사업장'의 기간 매출 */
    @Query("SELECT COALESCE(SUM(st.salesAmount), 0) FROM SalesTransaction st " +
           "WHERE st.store.storeId = :storeId " + 
           "AND st.transactionTime >= :start AND st.transactionTime < :end")
    BigDecimal sumSalesAmountByStoreIdBetween(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /** * ⭐️ (수정) 대시보드용: 특정 사업장의 최근 거래 1건 (시간 조회용)
     * (Store_StoreId -> StoreStoreId 카멜 케이스로 수정)
     */
    Optional<SalesTransaction> findTopByStoreStoreIdOrderByTransactionTimeDesc(Long storeId);
}