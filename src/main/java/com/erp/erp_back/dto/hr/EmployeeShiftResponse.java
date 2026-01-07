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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeShiftResponse {

    private Long shiftId;
    private Long storeId;
    private Long employeeId;
    private String employeeName;   // UI에 표시하기 좋게
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isFixed;
    private Integer breakMinutes;
    private Long shiftGroupId; // 동일한 스케줄 묶음 식별용 (옵션)
}