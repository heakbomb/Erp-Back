package com.erp.erp_back.dto.log;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사장페이지 - 직원별 출결 요약 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceSummary {

    private Long employeeId;
    private String employeeName;

    private Long storeId;
    private String storeName; // 필요 없으면 나중에 제거

    private LocalDate date;           // 요약 기준 날짜

    private String status;            // "ABSENT" / "WORKING" / "OUT"
    private LocalDateTime firstIn;    // 첫 출근 시간
    private LocalDateTime lastOut;    // 마지막 퇴근 시간

    private Long workedMinutes;       // 해당 날짜 총 근무 분(대략, 휴게 미반영 가능)
    private Long logCount;            // 기록 건수

    // 지각 기준 시간은 나중에 추가 예정
    // private boolean late;
}