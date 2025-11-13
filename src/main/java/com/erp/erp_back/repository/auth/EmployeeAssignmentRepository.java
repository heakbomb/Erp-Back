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
                                             
    // ⭐️ [수정] (Store_StoreId -> StoreStoreId) 카멜 케이스로 변경
    long countByStoreStoreIdAndStatus(Long storeId, String status);
}