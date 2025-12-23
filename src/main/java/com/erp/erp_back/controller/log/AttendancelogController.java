package com.erp.erp_back.controller.log;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.hr.AttendanceShiftStatusResponse;
import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.service.log.AttendancelogService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/attendance")

@RequiredArgsConstructor
public class AttendancelogController {

    private final AttendancelogService service;

    /** 출퇴근 기록 */
    @PostMapping("/punch")
    public ResponseEntity<AttendanceLogResponse> punch(@RequestBody AttendanceLogRequest req) {
        // QR 기반만 사용하므로 바로 서비스 호출
        return ResponseEntity.ok(service.punch(req));
    }

    /** 최근 기록 (직원+매장 기준) */
    @GetMapping("/recent")
    public ResponseEntity<List<AttendanceLogResponse>> recent(@RequestParam Long employeeId,
                                                              @RequestParam Long storeId) {
        return ResponseEntity.ok(service.recent(employeeId, storeId));
    }

    /** 일자별 기록 (직원+매장 기준) */
    @GetMapping("/day")
    public ResponseEntity<List<AttendanceLogResponse>> byDay(@RequestParam Long employeeId,
                                                             @RequestParam Long storeId,
                                                             @RequestParam
                                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                             LocalDate date) {
        return ResponseEntity.ok(service.byDay(employeeId, storeId, date));
    }

    // =========================
    // 직원 본인 조회용
    // =========================

    /** 직원 본인 최근 N건 (storeId 선택) */
    @GetMapping("/my/recent")
    public ResponseEntity<List<AttendanceLogResponse>> myRecent(
            @RequestParam Long employeeId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, defaultValue = "30") Integer limit
    ) {
        return ResponseEntity.ok(service.myRecent(employeeId, limit, storeId));
    }

    /** 직원 본인 기간 조회 (storeId 선택) */
    @GetMapping("/my/range")
    public ResponseEntity<List<AttendanceLogResponse>> myRange(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long storeId
    ) {
        return ResponseEntity.ok(service.myRange(employeeId, from, to, storeId));
    }

    // =========================
    // 사장페이지용 - 매장 출퇴근 로그 조회
    // =========================
    @GetMapping("/owner/logs")
    public ResponseEntity<List<AttendanceLogResponse>> ownerLogs(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long employeeId
    ) {
        return ResponseEntity.ok(
                service.findLogsForOwner(storeId, from, to, employeeId)
        );
    }

    /** 단일 날짜 편의용 (기존 기능 유지) */
    @GetMapping("/logs")
    public ResponseEntity<List<AttendanceLogResponse>> logsByDate(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long employeeId
    ) {
        return ResponseEntity.ok(
                service.findLogsForOwner(storeId, date, date, employeeId)
        );
    }

    // =========================
    // 사장페이지용 - 직원 출결 "월간 요약" 조회
    // =========================
    @GetMapping("/owner/summary")
    public ResponseEntity<List<EmployeeAttendanceSummary>> ownerMonthlySummary(
            @RequestParam Long storeId,
            // month: "2025-11" 형식
            @RequestParam String month,
            @RequestParam(required = false) Long employeeId
    ) {
        return ResponseEntity.ok(
                service.findMonthlySummary(storeId, month, employeeId)
        );
    }

    // =========================
    // ✅ [추가] shift 상태 API
    // =========================
    @GetMapping("/shift/status")
    public ResponseEntity<AttendanceShiftStatusResponse> shiftStatus(
            @RequestParam Long employeeId,
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(service.getShiftStatus(employeeId, storeId));
    }

    // 공통 에러 응답
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }
}