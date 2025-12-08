package com.erp.erp_back.repository.hr;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.hr.PayrollHistory;

public interface PayrollHistoryRepository extends JpaRepository<PayrollHistory, Long> {

    // 한 매장의 전체 지급 내역 (최근 월 순으로 보고 싶을 때)
    // 🔥 yearMonth → payrollMonth 로 변경
    List<PayrollHistory> findByStore_StoreIdOrderByPayrollMonthDesc(Long storeId);

    // 한 매장의 특정 월 지급 내역
    // 🔥 AndYearMonth → AndPayrollMonth 로 변경
    List<PayrollHistory> findByStore_StoreIdAndPayrollMonthOrderByEmployee_EmployeeIdAsc(
            Long storeId,
            String yearMonth   // 파라미터 이름은 그대로 yearMonth 써도 됨
    );

    // 한 매장 + 한 직원 + 한 달 : upsert용
    // 🔥 AndYearMonth → AndPayrollMonth 로 변경
    Optional<PayrollHistory> findByStore_StoreIdAndEmployee_EmployeeIdAndPayrollMonth(
            Long storeId,
            Long employeeId,
            String yearMonth   // 마찬가지로 변수 이름은 상관 없음
    );

     // ✅ 수정된 버전: 엔티티 필드명에 맞게 payrollMonth 사용
    List<PayrollHistory> findByStore_StoreIdAndEmployee_EmployeeIdOrderByPayrollMonthDesc(
            Long storeId,
            Long employeeId
    );
}