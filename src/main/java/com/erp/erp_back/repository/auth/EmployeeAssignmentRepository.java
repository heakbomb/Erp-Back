package com.erp.erp_back.repository.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.entity.auth.EmployeeAssignment;

public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, Long> {

  Optional<EmployeeAssignment> findFirstByEmployee_EmployeeIdAndStore_StoreIdAndStatusIn(
      Long employeeId, Long storeId, Collection<String> statuses);

  @Query("""
      select a
      from EmployeeAssignment a
      join fetch a.employee e
      where a.store.storeId = :storeId
        and a.status = 'PENDING'
      """)
  List<EmployeeAssignment> findPendingByStoreId(@Param("storeId") Long storeId);

  boolean existsByStore_StoreId(Long storeId);

  void deleteByStore_StoreId(Long storeId);

  @Query("""
          select count(a) > 0
          from EmployeeAssignment a
          where a.employee.employeeId = :employeeId
            and a.store.storeId = :storeId
            and a.status = 'APPROVED'
      """)
  boolean existsApprovedByEmployeeAndStore(@Param("employeeId") Long employeeId,
      @Param("storeId") Long storeId);

  long countByStoreStoreIdAndStatus(Long storeId, String status);

  // ⭐️ [신규 추가] 특정 매장의 '모든' 직원 목록 조회 (상태 무관)
  // N+1 문제 방지를 위해 JOIN FETCH 사용
  @Query("SELECT ea FROM EmployeeAssignment ea JOIN FETCH ea.employee WHERE ea.store.storeId = :storeId")
  List<EmployeeAssignment> findAllByStoreId(@Param("storeId") Long storeId);


  @Query("""
    SELECT ea
    FROM EmployeeAssignment ea
    JOIN FETCH ea.employee e
    JOIN FETCH ea.store s
    WHERE ea.store.storeId = :storeId
      AND ea.status = 'APPROVED'
    """)
List<EmployeeAssignment> findApprovedByStoreId(@Param("storeId") Long storeId);

// ✅ 급여용: 특정 매장(storeId)에 배정된 모든 직원 조회
    List<EmployeeAssignment> findByStore_StoreId(Long storeId);
}