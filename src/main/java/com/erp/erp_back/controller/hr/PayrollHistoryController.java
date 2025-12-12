package com.erp.erp_back.controller.hr;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.hr.PayrollHistoryDto;
import com.erp.erp_back.dto.hr.PayrollHistoryDetailDto;
import com.erp.erp_back.entity.hr.PayrollRun;          // ✅ (추가)
import com.erp.erp_back.service.hr.PayrollHistoryService;
import com.erp.erp_back.service.hr.PayrollRunService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/owner/payroll/history")
@RequiredArgsConstructor
@Slf4j
public class PayrollHistoryController {

    private final PayrollHistoryService payrollHistoryService;
    private final PayrollRunService payrollRunService;

    private YearMonth parseYearMonthParam(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("yearMonth parameter is empty");
        }
        String ymStr = raw.length() >= 7 ? raw.substring(0, 7) : raw;
        return YearMonth.parse(ymStr);
    }

    @PostMapping("/save")
    public ResponseEntity<List<PayrollHistoryDetailDto>> saveMonthlyHistory(
            @RequestParam Long storeId,
            @RequestParam String yearMonth
    ) {
        YearMonth ym = parseYearMonthParam(yearMonth);

        List<PayrollHistoryDetailDto> result =
                payrollRunService.runManual(storeId, ym);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<PayrollHistoryDetailDto>> getMonthlyHistory(
            @RequestParam Long storeId,
            @RequestParam String yearMonth
    ) {
        YearMonth ym = parseYearMonthParam(yearMonth);
        List<PayrollHistoryDetailDto> result =
                payrollHistoryService.getMonthlyHistory(storeId, ym);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{payrollId}/status")
    public ResponseEntity<PayrollHistoryDetailDto> updateStatus(
            @PathVariable Long payrollId,
            @RequestParam String status
    ) {
        System.out.println("[PayrollHistory] updateStatus id=" + payrollId + ", status=" + status);

        PayrollHistoryDetailDto dto = payrollHistoryService.updateStatus(payrollId, status);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PayrollHistoryDto>> getHistorySummary(
            @RequestParam Long storeId
    ) {
        List<PayrollHistoryDto> result = payrollHistoryService.getHistorySummary(storeId);
        return ResponseEntity.ok(result);
    }

    // ✅ (추가) payroll_run 상태 조회: 프론트가 FINALIZED 여부 확인용
    // GET /owner/payroll/history/run?storeId=11&yearMonth=2025-11
    @GetMapping("/run")
    public ResponseEntity<Map<String, Object>> getRunStatus(
            @RequestParam Long storeId,
            @RequestParam String yearMonth
    ) {
        YearMonth ym = parseYearMonthParam(yearMonth);

        PayrollRun run = payrollRunService.getRunOrNull(storeId, ym);

        Map<String, Object> body = new HashMap<>();
        body.put("exists", run != null);
        body.put("status", run != null ? run.getStatus() : "NONE");
        body.put("finalizedAt", run != null ? run.getFinalizedAt() : null);
        body.put("source", run != null ? run.getSource() : null);
        body.put("version", run != null ? run.getVersion() : 0);

        return ResponseEntity.ok(body);
    }
}