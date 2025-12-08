package com.erp.erp_back.repository.store;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.entity.store.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

  // ✅ [수정됨] fetch 위치를 'join' 바로 뒤로 이동했습니다.
  // (기존: join s.businessNumber bn fetch -> 수정: join fetch s.businessNumber bn)
  @Query("""
      select s
      from Store s
        join fetch s.businessNumber bn
        join bn.owner o
      where o.ownerId = :ownerId
      order by s.storeId asc
      """)
  List<Store> findAllByOwnerId(@Param("ownerId") Long ownerId);

  long countByStatus(String status);

  // ✅ 사장님 대시보드용 통계 쿼리 (이 부분은 문법적으로 괜찮습니다)
  @Query("""
      SELECT new com.erp.erp_back.dto.admin.AdminStoreDashboardItem(
          s.storeId,
          s.storeName,
          s.industry,
          s.status,
          (SELECT COUNT(ea) FROM EmployeeAssignment ea WHERE ea.store = s AND ea.status = 'APPROVED'),
          (SELECT COALESCE(SUM(st.totalAmount), 0) FROM SalesTransaction st WHERE st.store = s AND st.transactionTime BETWEEN :start AND :end),
          (SELECT MAX(st2.transactionTime) FROM SalesTransaction st2 WHERE st2.store = s),
          s.businessNumber.owner.username,
          s.businessNumber.owner.email,
          s.businessNumber.bizNum
      )
      FROM Store s
      WHERE s.businessNumber.owner.ownerId = :ownerId
      ORDER BY s.storeId ASC
      """)
  List<AdminStoreDashboardItem> findDashboardItemsByOwnerId(
      @Param("ownerId") Long ownerId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

    // ✅ 여기 추가: 특정 owner의 비활성 매장만 조회
    List<Store> findAllByBusinessNumber_Owner_OwnerIdAndStatus(Long ownerId, String status);
}