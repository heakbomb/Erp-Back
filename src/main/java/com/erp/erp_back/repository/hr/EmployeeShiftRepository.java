package com.erp.erp_back.repository.hr;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.hr.EmployeeShift;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {

    List<EmployeeShift> findByStore_StoreIdAndShiftDateBetween(Long storeId, LocalDate start, LocalDate end);

    List<EmployeeShift> findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(
            Long storeId, Long employeeId, LocalDate start, LocalDate end
    );

    List<EmployeeShift> findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDate(
            Long storeId, Long employeeId, LocalDate shiftDate
    );

    boolean existsByEmployee_EmployeeIdAndShiftDateAndStartTime(Long employeeId, LocalDate shiftDate, LocalTime startTime);

    void deleteByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(Long storeId, Long employeeId, LocalDate start, LocalDate end);

    // ✅ 추가
    List<EmployeeShift> findByShiftGroupId(Long shiftGroupId);

    // ✅ 추가
    void deleteByShiftGroupId(Long shiftGroupId);

    // ✅ 추가: 그룹 단위 검증/조회 시 store/employee까지 같이 쓰면 안전(옵션이지만 추천)
    List<EmployeeShift> findByShiftGroupIdAndStore_StoreId(Long shiftGroupId, Long storeId);
}