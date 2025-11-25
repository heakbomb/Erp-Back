package com.erp.erp_back.dto.hr;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeShiftBulkRequest {

    @NotNull
    private Long storeId;

    @NotNull
    private Long employeeId;

    /** 한 번에 생성/수정할 날짜들 */
    @NotEmpty
    private List<LocalDate> dates;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    /** 휴게시간(분). 없으면 0 */
    private Integer breakMinutes;

    /** 고정 스케줄 여부 */
    private Boolean isFixed = false;
}