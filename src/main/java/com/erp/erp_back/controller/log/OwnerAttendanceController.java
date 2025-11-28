package com.erp.erp_back.controller.log;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.service.log.OwnerAttendanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/attendance")
@RequiredArgsConstructor
public class OwnerAttendanceController {

    private final OwnerAttendanceService ownerAttendanceService;

    /**
     * 사장페이지 - 직원 출결 현황 (일간 요약)
     *
     * 예: GET /owner/attendance/summary?storeId=11&date=2025-11-26
     * date 를 안 보내면 오늘 기준
     */
    @GetMapping("/summary")
    public ResponseEntity<List<EmployeeAttendanceSummary>> summary(
            @RequestParam Long storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        List<EmployeeAttendanceSummary> list =
                ownerAttendanceService.getDailySummary(storeId, date);
        return ResponseEntity.ok(list);
    }
}