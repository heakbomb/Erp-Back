package com.erp.erp_back.dto.hr;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceShiftStatusResponse {

    private Long storeId;
    private Long employeeId;

    // 기준 날짜/시간(서버 기준)
    private LocalDate date;
    private LocalTime now;

    // 선택된(현재 혹은 다음) shift
    private Long shiftId;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;

    // 로그 상태
    private boolean hasIn;
    private boolean hasOut;

    // 버튼 상태
    private boolean canClockIn;
    private boolean canClockOut;

    // 안내 메시지
    private String message;
}