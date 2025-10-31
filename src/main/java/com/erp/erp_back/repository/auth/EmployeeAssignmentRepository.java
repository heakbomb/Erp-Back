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

    // ✅ FK 존재 여부 확인
    boolean existsByStore_StoreId(Long storeId);

    // ✅ 자식 먼저 일괄 삭제
    void deleteByStore_StoreId(Long storeId);
}