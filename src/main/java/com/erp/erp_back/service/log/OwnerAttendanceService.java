package com.erp.erp_back.service.log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.dto.log.EmployeeStatusSummary;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.mapper.EmployeeAttendanceMapper;
import com.erp.erp_back.repository.log.AttendanceLogRepository;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerAttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final EmployeeAttendanceMapper attendanceMapper;

    /**
     * 사장페이지 - 특정 매장의 특정 날짜 직원 출결 요약 목록
     */
    public List<EmployeeAttendanceSummary> getDailySummary(Long storeId, LocalDate date) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId는 필수입니다.");
        }
        if (date == null) {
            date = LocalDate.now();
        }

        // 1) 매장에 승인된 직원 목록
        List<EmployeeAssignment> assignments =
                employeeAssignmentRepository.findApprovedByStoreId(storeId);

        if (assignments.isEmpty()) {
            return List.of();
        }

        // 2) 해당 매장의 해당 날짜 전체 출결 로그
        List<AttendanceLog> logs = attendanceLogRepository.findByStoreAndDate(storeId, date);

        // employeeId → 로그 리스트 맵핑
        Map<Long, List<AttendanceLog>> logsByEmployee = new HashMap<>();
        for (AttendanceLog log : logs) {
            Long empId = log.getEmployee().getEmployeeId();
            logsByEmployee
                    .computeIfAbsent(empId, k -> new ArrayList<>())
                    .add(log);
        }

        // 3) 직원별 DTO 생성
        List<EmployeeAttendanceSummary> result = new ArrayList<>();
        for (EmployeeAssignment ea : assignments) {
            var employee = ea.getEmployee();
            var store = ea.getStore();
            Long empId = employee.getEmployeeId();

            List<AttendanceLog> empLogs = logsByEmployee.getOrDefault(empId, List.of());

            EmployeeAttendanceSummary dto =
                    attendanceMapper.toSummary(employee, store, date, empLogs);

            result.add(dto);
        }

        return result;
    }

    public EmployeeStatusSummary getEmployeeStatus(Long storeId) {
    if (storeId == null) {
        throw new IllegalArgumentException("storeId는 필수입니다.");
    }

    LocalDate today = LocalDate.now();

    // 오늘 이 매장의 로그
    List<AttendanceLog> logs =
            attendanceLogRepository.findByStoreAndDate(storeId, today);

    Map<Long, AttendanceLog> latestLogByEmp = new HashMap<>();
    for (AttendanceLog log : logs) {
        if (log.getEmployee() == null || log.getRecordTime() == null) continue;

        Long empId = log.getEmployee().getEmployeeId();
        AttendanceLog current = latestLogByEmp.get(empId);

        if (current == null ||
            log.getRecordTime().isAfter(current.getRecordTime())) {
            latestLogByEmp.put(empId, log);
        }
    }

    long workingCount = latestLogByEmp.values().stream()
            .filter(l -> "IN".equalsIgnoreCase(l.getRecordType()))
            .count();

    List<EmployeeAssignment> assignments =
            employeeAssignmentRepository.findApprovedByStoreId(storeId);
    long totalCount = assignments.size();

    return new EmployeeStatusSummary(workingCount, totalCount);
}
}