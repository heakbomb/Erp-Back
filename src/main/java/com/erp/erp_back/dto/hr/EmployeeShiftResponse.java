package com.erp.erp_back.dto.hr;

import java.time.LocalDate;
import java.time.LocalTime;

import com.erp.erp_back.entity.hr.EmployeeShift;

import lombok.*;

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

    public static EmployeeShiftResponse from(EmployeeShift s) {
        return EmployeeShiftResponse.builder()
                .shiftId(s.getShiftId())
                .storeId(s.getStore().getStoreId())
                .employeeId(s.getEmployee().getEmployeeId())
                .employeeName(s.getEmployee().getName()) // Employee 엔티티에 name 있다고 가정
                .shiftDate(s.getShiftDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .isFixed(s.getIsFixed())
                .breakMinutes(s.getBreakMinutes())
                .build();
    }
}