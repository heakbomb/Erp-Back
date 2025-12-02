// src/main/java/com/erp/erp_back/service/hr/OwnerPayrollService.java
package com.erp.erp_back.service.hr;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.OwnerPayrollResponse;
import com.erp.erp_back.dto.hr.OwnerPayrollResponse.EmployeePayroll;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerPayrollService {

    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final AttendanceLogRepository attendanceLogRepository;

    @Transactional(readOnly = true)
    public OwnerPayrollResponse getMonthlyPayroll(Long storeId, YearMonth yearMonth) {

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);

        // 1) 이 매장에 배정된 전체 직원 (필요하면 나중에 status=APPROVED 조건 추가)
        List<EmployeeAssignment> assignments =
            employeeAssignmentRepository.findByStore_StoreId(storeId);

        // 2) 해당 기간의 전체 출결 로그
        List<AttendanceLog> logs =
            attendanceLogRepository.findByStoreAndDateTimeRange(storeId, from, to);

        // 2-1) 직원별로 로그 그룹핑
        Map<Long, List<AttendanceLog>> logsByEmp = new HashMap<>();
        for (AttendanceLog log : logs) {
            if (log.getEmployee() == null) continue;
            Long empId = log.getEmployee().getEmployeeId();
            logsByEmp.computeIfAbsent(empId, k -> new ArrayList<>()).add(log);
        }

        // 2-2) 직원별 근무일수 / 근무시간(분) 계산
        Map<Long, Long> workDaysMap = new HashMap<>();    // employeeId -> 근무일수
        Map<Long, Long> workMinutesMap = new HashMap<>(); // employeeId -> 근무시간(분)

        for (Map.Entry<Long, List<AttendanceLog>> entry : logsByEmp.entrySet()) {
            Long empId = entry.getKey();
            List<AttendanceLog> empLogs = entry.getValue();

            // 시간 순 정렬
            empLogs.sort(Comparator.comparing(AttendanceLog::getRecordTime));

            Set<LocalDate> days = new HashSet<>();
            long totalMinutes = 0L;
            LocalDateTime lastIn = null;

            for (AttendanceLog l : empLogs) {
                LocalDateTime time = l.getRecordTime();
                if (time == null) continue;

                days.add(time.toLocalDate());

                String type = l.getRecordType();
                if ("IN".equalsIgnoreCase(type)) {
                    lastIn = time;
                } else if ("OUT".equalsIgnoreCase(type) && lastIn != null) {
                    totalMinutes += Duration.between(lastIn, time).toMinutes();
                    lastIn = null;
                }
            }

            workDaysMap.put(empId, (long) days.size());
            workMinutesMap.put(empId, totalMinutes);
        }

        // 3) 급여 화면에 내려줄 직원별 DTO 만들기
        List<EmployeePayroll> employees = assignments.stream()
            .map(assign -> {
                Long empId = assign.getEmployee().getEmployeeId();

                long workDays = workDaysMap.getOrDefault(empId, 0L);
                long workMinutes = workMinutesMap.getOrDefault(empId, 0L);
                double workHours = workMinutes / 60.0;  // 🔥 분 → 시간 변환

                // 급여 금액 계산 로직은 아직 없으니 일단 0으로 채워 둠
                long hourlyWage = 0L;
                long basePay = 0L;
                long bonus = 0L;
                long deductions = 0L;
                long netPay = basePay + bonus - deductions;

                return new EmployeePayroll(
                    empId,
                    assign.getEmployee().getName(),
                    assign.getRole(),
                    workDays,
                    workHours,
                    hourlyWage,
                    basePay,
                    bonus,
                    deductions,
                    netPay,
                    "예정"
                );
            })
            .toList();

        // 4) 급여 이력은 아직 테이블 없으니 빈 리스트로 내려줌
        return new OwnerPayrollResponse(employees, List.of());
    }
}