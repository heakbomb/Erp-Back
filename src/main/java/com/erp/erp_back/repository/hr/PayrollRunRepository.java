package com.erp.erp_back.repository.hr;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.erp_back.entity.hr.PayrollRun;

import jakarta.persistence.LockModeType;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    Optional<PayrollRun> findByStore_StoreIdAndPayrollMonth(Long storeId, String payrollMonth);

    // ✅ 동시 실행(스케줄러/수동) 충돌 방지용: 행 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PayrollRun r where r.store.storeId = :storeId and r.payrollMonth = :payrollMonth")
    Optional<PayrollRun> findForUpdate(@Param("storeId") Long storeId, @Param("payrollMonth") String payrollMonth);
}