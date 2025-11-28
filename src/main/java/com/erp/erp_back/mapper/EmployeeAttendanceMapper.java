package com.erp.erp_back.mapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

@Component
public class EmployeeAttendanceMapper {

    /**
     * 한 직원의 "하루" 출결 로그들을
     * - 근무일수(이 날 근무했으면 1, 아니면 0)
     * - 근무시간(시간 단위)
     * 로 변환해서 월간 요약 DTO 형태로 만들어줌.
     *
     * 이 메서드는 하루 단위로 합산된 값을 리턴하고,
     * 월간 합산은 Service 쪽에서 여러 날을 더하는 식으로 사용하면 됨.
     */
    public EmployeeAttendanceSummary toSummary(
            Employee employee,
            Store store,
            LocalDate date,
            List<AttendanceLog> logsForDay
    ) {
        // 기본값: 근무 안 한 날
        int workDays = 0;
        double workHours = 0.0;

        if (logsForDay != null && !logsForDay.isEmpty()) {
            workDays = 1; // 이 날짜에 로그가 한 개라도 있으면 근무일 1일

            // 시간 순 정렬이 되어있다고 가정하지만, 혹시 몰라 정렬 한 번 더
            logsForDay.sort((a, b) -> a.getRecordTime().compareTo(b.getRecordTime()));

            LocalDateTime firstIn = null;
            LocalDateTime lastOut = null;

            for (AttendanceLog log : logsForDay) {
                if ("IN".equalsIgnoreCase(log.getRecordType())) {
                    if (firstIn == null) {
                        firstIn = log.getRecordTime();
                    }
                }
                if ("OUT".equalsIgnoreCase(log.getRecordType())) {
                    lastOut = log.getRecordTime();
                }
            }

            long minutes = 0L;
            if (firstIn != null && lastOut != null) {
                minutes = Duration.between(firstIn, lastOut).toMinutes();
            }
            workHours = minutes / 60.0;
        }

        // 이제 하루치 요약을 EmployeeAttendanceSummary 형태로 반환
        EmployeeAttendanceSummary dto = new EmployeeAttendanceSummary();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setEmployeeName(employee.getName());
        dto.setStoreId(store.getStoreId());
        dto.setStoreName(store.getStoreName());

        dto.setWorkDaysThisMonth(workDays);
        dto.setWorkHoursThisMonth(workHours);

        return dto;
    }
}