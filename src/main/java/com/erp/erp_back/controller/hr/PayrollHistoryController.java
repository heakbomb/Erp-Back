package com.erp.erp_back.controller.hr;

import java.time.YearMonth;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.erp.erp_back.dto.hr.PayrollHistoryDto;
import com.erp.erp_back.dto.hr.PayrollHistoryDetailDto;
import com.erp.erp_back.service.hr.PayrollHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/owner/payroll/history")
@RequiredArgsConstructor
@Slf4j
public class PayrollHistoryController {

    private final PayrollHistoryService payrollHistoryService;

    // yearMonth 문자열을 안전하게 YearMonth로 변환하는 헬퍼
    private YearMonth parseYearMonthParam(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("yearMonth parameter is empty");
        }
        String ymStr = raw.length() >= 7 ? raw.substring(0, 7) : raw;
        return YearMonth.parse(ymStr);  // yyyy-MM 포맷만 남겨서 파싱
    }

    /**
     * ✅ 이번 달(or 선택한 달) 급여를 계산해서
     *    payroll_history 테이블에 저장/업데이트 하고 결과를 반환
     */
    @PostMapping("/save")
    public ResponseEntity<List<PayrollHistoryDetailDto>> saveMonthlyHistory(
            @RequestParam Long storeId,
            @RequestParam String yearMonth
    ) {
        YearMonth ym = parseYearMonthParam(yearMonth);
        List<PayrollHistoryDetailDto> result =
                payrollHistoryService.saveMonthlyHistory(storeId, ym);

        return ResponseEntity.ok(result);
    }

    /**
     * ✅ 특정 월의 급여 지급 내역 조회
     */
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

    /**
     * ✅ 급여 지급 상태 업데이트
     */
    @PatchMapping("/{payrollId}/status")
    public ResponseEntity<PayrollHistoryDetailDto> updateStatus(
            @PathVariable Long payrollId,
            @RequestParam String status
    ) {
        System.out.println("[PayrollHistory] updateStatus id=" + payrollId + ", status=" + status);

        PayrollHistoryDetailDto dto = payrollHistoryService.updateStatus(payrollId, status);

        return ResponseEntity.ok(dto);
    }
    
    /**
     * ✅ 매장 전체 급여 지급 요약 (월별)
     *
     *  - GET /owner/payroll/history/summary?storeId=1
     *  - 응답: List<PayrollHistoryDto>
     */
    @GetMapping("/summary")
    public ResponseEntity<List<PayrollHistoryDto>> getHistorySummary(
            @RequestParam Long storeId
    ) {
        List<PayrollHistoryDto> result = payrollHistoryService.getHistorySummary(storeId);
        return ResponseEntity.ok(result);
    }
}