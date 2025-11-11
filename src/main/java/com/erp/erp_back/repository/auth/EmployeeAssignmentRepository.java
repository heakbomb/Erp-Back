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

    // ✅ FK 존재 여부 확인 (스토어 삭제 전 체크/정리용)
    boolean existsByStore_StoreId(Long storeId);

    // ✅ 자식 먼저 일괄 삭제 (스토어 삭제 전 정리용)
    void deleteByStore_StoreId(Long storeId);

    // ✅ 근태 권한 확인: 해당 직원이 해당 매장에 승인 상태인지
    @Query("""
        select count(a) > 0
        from EmployeeAssignment a
        where a.employee.employeeId = :employeeId
          and a.store.storeId = :storeId
          and a.status = 'APPROVED'
    """)
    boolean existsApprovedByEmployeeAndStore(@Param("employeeId") Long employeeId,
                                             @Param("storeId") Long storeId);
}