package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}