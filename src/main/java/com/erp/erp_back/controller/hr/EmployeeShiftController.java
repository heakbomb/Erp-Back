package com.erp.erp_back.controller.hr;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.hr.ShiftApiResult;
import com.erp.erp_back.dto.hr.EmployeeShiftBulkRequest;
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

    @PostMapping
    public ResponseEntity<ShiftApiResult<EmployeeShiftResponse>> upsertShift(
            @Valid @RequestBody EmployeeShiftUpsertRequest req
    ) {
        try {
            EmployeeShiftResponse saved = shiftService.upsertShift(req);
            return ResponseEntity.ok(ShiftApiResult.ok(saved));
        } catch (IllegalStateException e) {
            // ✅ [수정] 하드코딩 제거: 서비스에서 던진 메시지를 그대로 내려서 프론트 alert 가능
            return ResponseEntity.ok(ShiftApiResult.duplicate(e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<ShiftApiResult<List<EmployeeShiftResponse>>> upsertBulk(
            @Valid @RequestBody EmployeeShiftBulkRequest req
    ) {
        try {
            List<EmployeeShiftResponse> saved = shiftService.upsertBulk(req);
            return ResponseEntity.ok(ShiftApiResult.ok(saved));
        } catch (IllegalStateException e) {
            // ✅ [수정] 동일
            return ResponseEntity.ok(ShiftApiResult.duplicate(e.getMessage()));
        }
    }

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> delete(@PathVariable Long shiftId) {
        shiftService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

     // ✅ 묶음 수정 (야간 2조각 동시)
    @PutMapping("/group/{shiftGroupId}")
    public ResponseEntity<ShiftApiResult<List<EmployeeShiftResponse>>> updateGroup(
            @RequestParam Long storeId,
            @PathVariable Long shiftGroupId,
            @Valid @RequestBody EmployeeShiftUpsertRequest req
    ) {
        try {
            List<EmployeeShiftResponse> saved = shiftService.updateGroup(storeId, shiftGroupId, req);
            return ResponseEntity.ok(ShiftApiResult.ok(saved));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ShiftApiResult.duplicate(e.getMessage()));
        }
    }

    // ✅ 묶음 삭제
    @DeleteMapping("/group/{shiftGroupId}")
    public ResponseEntity<Void> deleteGroup(
            @RequestParam Long storeId,
            @PathVariable Long shiftGroupId
    ) {
        shiftService.deleteGroup(storeId, shiftGroupId);
        return ResponseEntity.noContent().build();
    }
}