package com.erp.erp_back.repository.erp;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.erp.SalesLineItem;

@Repository
public interface SalesLineItemRepository extends JpaRepository<SalesLineItem, Long> {

    List<SalesLineItem> findBySalesTransactionTransactionId(Long transactionId);

    @Query("SELECT new com.erp.erp_back.dto.erp.TopMenuStatsResponse(" +
           "  m.menuId, m.menuName, SUM(li.quantity), SUM(li.lineAmount)) " +
           "FROM SalesLineItem li " +
           "JOIN li.salesTransaction t " +
           "JOIN li.menuItem m " +
           "WHERE t.store.storeId = :storeId " +
           "AND t.transactionTime BETWEEN :from AND :to " +
           "AND t.status = 'PAID' " +  
           "GROUP BY m.menuId, m.menuName " +
           "ORDER BY SUM(li.lineAmount) DESC")
    List<TopMenuStatsResponse> findTopMenuStats(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}