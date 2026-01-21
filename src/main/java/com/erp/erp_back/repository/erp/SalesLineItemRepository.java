package com.erp.erp_back.repository.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.dto.erp.DailyMenuStatDto;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.erp.SalesLineItem;

@Repository
public interface SalesLineItemRepository extends JpaRepository<SalesLineItem, Long> {

       List<SalesLineItem> findBySalesTransactionTransactionId(Long transactionId);

       // [기존 메서드 유지]
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
                     @Param("to") LocalDateTime to);

       // ▼▼▼ [이 부분 추가해주세요] 스케줄러용 대량 집계 메서드 ▼▼▼
       @Query("SELECT " +
                     "  t.store.storeId as storeId, " +
                     "  m.menuId as menuId, " +
                     "  SUM(li.quantity) as totalQuantity, " +
                     "  SUM(li.lineAmount) as totalAmount " +
                     "FROM SalesLineItem li " +
                     "JOIN li.salesTransaction t " +
                     "JOIN li.menuItem m " +
                     "WHERE t.transactionTime BETWEEN :start AND :end " +
                     "AND t.status = 'PAID' " +
                     "GROUP BY t.store.storeId, m.menuId")
       List<DailyMenuStatDto> findDailyMenuStatsByDate(
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

       public interface CategorySalesRow {
              String getCategory();

              BigDecimal getSales();
       }

       // 예: SalesLineItem이 transaction/store/menuItem을 참조하는 경우
       @Query("""
        SELECT li.menuItem.categoryName as category,
               SUM(li.lineAmount) as sales
        FROM SalesLineItem li
        WHERE li.salesTransaction.store.storeId = :storeId
          AND li.salesTransaction.transactionTime BETWEEN :from AND :to
          AND li.salesTransaction.status = 'PAID'
        GROUP BY li.menuItem.categoryName
        ORDER BY SUM(li.lineAmount) DESC
    """)
    List<CategorySalesRow> findCategorySales(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
