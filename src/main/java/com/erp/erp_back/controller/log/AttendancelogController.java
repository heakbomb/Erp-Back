package com.erp.erp_back.controller.log;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.service.log.AttendancelogService;
import com.erp.erp_back.support.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AttendancelogController {

    private final AttendancelogService service;

    /** 출퇴근 기록 */
    @PostMapping("/punch")
    public ResponseEntity<AttendanceLogResponse> punch(@RequestBody AttendanceLogRequest req,
                                                       HttpServletRequest http) {
        String ip = IpUtils.getClientIp(http);
        return ResponseEntity.ok(service.punch(req, ip));
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