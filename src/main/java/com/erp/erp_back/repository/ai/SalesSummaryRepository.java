package com.erp.erp_back.repository.ai;

import com.erp.erp_back.dto.ai.SalesSummaryDto;
import com.erp.erp_back.entity.erp.SalesMenuDailySummary; // 사용자님 패키지 경로
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesSummaryRepository extends JpaRepository<SalesMenuDailySummary, Long> {

    @Query("SELECT new com.erp.back.dto.ai.SalesSummaryDto(" +
           "  FUNCTION('DATE_FORMAT', s.summaryDate, '%Y-%m-%d'), " +
           "  s.store.id, " +
           "  s.menuItem.id, " +
           "  s.totalQuantity, " +
           "  s.totalAmount, " +
           // ❌ s.totalDiscount 삭제함
           "  s.menuItem.name, " +     // MenuItem에 name 필드가 있다고 가정
           "  s.menuItem.category, " + // MenuItem에 category 필드가 있다고 가정
           "  s.store.nx, " +
           "  s.store.ny) " +
           "FROM SalesMenuDailySummary s " +
           "JOIN s.store " +
           "JOIN s.menuItem " +
           "WHERE s.summaryDate BETWEEN :startDate AND :endDate")
    List<SalesSummaryDto> findRichSalesData(LocalDate startDate, LocalDate endDate);
}