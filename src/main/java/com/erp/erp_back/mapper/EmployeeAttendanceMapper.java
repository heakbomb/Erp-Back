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
     * 한 직원의 "하루" 출결 로그들을 요약해서 DTO로 변환
     */
    public EmployeeAttendanceSummary toSummary(
            Employee employee,
            Store store,
            LocalDate date,
            List<AttendanceLog> logsForDay
    ) {
        EmployeeAttendanceSummary dto = new EmployeeAttendanceSummary();

        dto.setEmployeeId(employee.getEmployeeId());
        dto.setEmployeeName(employee.getName());
        dto.setStoreId(store.getStoreId());
        dto.setStoreName(store.getStoreName());
        dto.setDate(date);

        dto.setLogCount((long) logsForDay.size());

        if (logsForDay.isEmpty()) {
            dto.setStatus("ABSENT");
            dto.setFirstIn(null);
            dto.setLastOut(null);
            dto.setWorkedMinutes(0L);
            return dto;
        }

        // 시간 순 정렬이 되어있다고 가정(Repository에서 order by)하지만
        // 혹시 몰라 한번 더 정렬해도 됨
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

        dto.setFirstIn(firstIn);
        dto.setLastOut(lastOut);

        // 상태 계산 (지금은 간단하게)
        // - 로그 없음       : ABSENT (위에서 처리)
        // - 마지막 로그가 IN : WORKING
        // - 마지막 로그가 OUT: OUT
        AttendanceLog lastLog = logsForDay.get(logsForDay.size() - 1);
        if ("IN".equalsIgnoreCase(lastLog.getRecordType())) {
            dto.setStatus("WORKING");
        } else {
            dto.setStatus("OUT");
        }

        // 근무 시간(분) 대략 계산: 첫 IN ~ 마지막 OUT
        Long minutes = 0L;
        if (firstIn != null && lastOut != null) {
            minutes = Duration.between(firstIn, lastOut).toMinutes();
        }
        dto.setWorkedMinutes(minutes);

        // 지각 여부는 추후 로직 추가 예정
        return dto;
    }
}