package com.erp.erp_back.repository.erp;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.SalesLineItem;

@Repository
public interface SalesLineItemRepository extends JpaRepository<SalesLineItem, Long> {

    List<SalesLineItem> findBySalesTransactionTransactionId(Long transactionId);

     @Query("""
        SELECT li.menuItem.menuId   AS menuId,
               li.menuItem.menuName AS name,
               SUM(li.quantity)     AS quantity,
               SUM(li.lineAmount)   AS revenue
        FROM SalesLineItem li
        JOIN li.salesTransaction tx
        WHERE tx.store.storeId = :storeId
          AND tx.transactionTime >= :start
          AND tx.transactionTime < :end
        GROUP BY li.menuItem.menuId, li.menuItem.menuName
        ORDER BY SUM(li.lineAmount) DESC
        """)
    List<Object[]> findMenuAggBetween(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}