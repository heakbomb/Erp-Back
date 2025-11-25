package com.erp.erp_back.dto.hr;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShiftUpsertRequest {

    private Long shiftId;               // 수정일 때만 사용, 생성이면 null

    @NotNull
    private Long storeId;

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate shiftDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    // ✅ 추가: 휴게시간(분). 안 넘기면 0
    private Integer breakMinutes;

    private Boolean isFixed = false;
}