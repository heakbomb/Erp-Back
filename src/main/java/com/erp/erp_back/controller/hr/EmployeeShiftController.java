package com.erp.erp_back.controller.hr;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.hr.EmployeeShiftBulkRequest;      // ✅ 추가
import com.erp.erp_back.dto.hr.EmployeeShiftResponse;
import com.erp.erp_back.dto.hr.EmployeeShiftUpsertRequest;
import com.erp.erp_back.service.hr.EmployeeShiftService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/shift")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmployeeShiftController {

    private final EmployeeShiftService shiftService;

    /**
     * ✅ 1) 특정 사업장의 월간 근무표 조회
     * GET /shift/monthly?storeId=1&year=2025&month=11
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<EmployeeShiftResponse>> getMonthlyByStore(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                shiftService.getMonthlyShiftsByStore(storeId, year, month)
        );
    }

    /**
     * ✅ 2) 특정 사업장 + 특정 직원의 월간 근무표
     * GET /shift/monthly/by-employee?storeId=1&employeeId=10&year=2025&month=11
     */
    @GetMapping("/monthly/by-employee")
    public ResponseEntity<List<EmployeeShiftResponse>> getMonthlyByEmployee(
            @RequestParam Long storeId,
            @RequestParam Long employeeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                shiftService.getMonthlyShiftsByEmployee(storeId, employeeId, year, month)
        );
    }

    /**
     * ✅ 3) 근무 스케줄 생성/수정 (단건)
     * POST /shift  (shiftId 없으면 생성, 있으면 수정)
     */
    @PostMapping
    public ResponseEntity<EmployeeShiftResponse> upsertShift(
            @Valid @RequestBody EmployeeShiftUpsertRequest req
    ) {
        return ResponseEntity.ok(shiftService.upsertShift(req));
    }

    /**
     * ✅ 4) 여러 날짜를 한 번에 생성/수정 (Bulk)
     * POST /shift/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<EmployeeShiftResponse>> upsertBulk(
            @Valid @RequestBody EmployeeShiftBulkRequest req
    ) {
        return ResponseEntity.ok(shiftService.upsertBulk(req));
    }

    /**
     * ✅ 5) 근무 스케줄 삭제
     * DELETE /shift/{shiftId}
     */
    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> delete(@PathVariable Long shiftId) {
        shiftService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ 6) 특정 사업장 + 직원 + 기간의 근무 스케줄 일괄 삭제
     * 예: DELETE /shift/range?storeId=1&employeeId=11&from=2025-12-01&to=2025-12-31
     */
    @DeleteMapping("/range")
    public ResponseEntity<Void> deleteRange(
            @RequestParam Long storeId,
            @RequestParam Long employeeId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        shiftService.deleteRange(storeId, employeeId, from, to);
        return ResponseEntity.noContent().build();
    }

    // 기본적인 에러 핸들링 (선택)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // ⭐ 중복 근무 스케줄 등 상태 충돌(IllegalStateException) 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        // 409 CONFLICT 로 내려주고, 메시지는 그대로 프론트에 전달
        return ResponseEntity.status(409).body(e.getMessage());
    }
}