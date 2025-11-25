package com.erp.erp_back.repository.hr;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.hr.EmployeeShift;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {

    // 1) 특정 가게 + 월 단위 전체 조회 (캘린더용)
    List<EmployeeShift> findByStore_StoreIdAndShiftDateBetween(
            Long storeId,
            LocalDate start,
            LocalDate end
    );

    // 2) 특정 직원 + 특정 가게 + 월 단위 조회 (직원별 상세)
    List<EmployeeShift> findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(
            Long storeId,
            Long employeeId,
            LocalDate start,
            LocalDate end
    );

    // ✅ 날짜별 존재 여부 확인용
    Optional<EmployeeShift> findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDate(
            Long storeId,
            Long employeeId,
            LocalDate shiftDate
    );
    // 직원 근무 등록시 중복 체크확인용
    boolean existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
            Long employeeId, LocalDate shiftDate, LocalTime startTime
    );

    // ✅ 새로 추가: 특정 사업장 + 직원 + 기간 전체 삭제
    void deleteByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(
            Long storeId,
            Long employeeId,
            LocalDate start,
            LocalDate end
    );
}