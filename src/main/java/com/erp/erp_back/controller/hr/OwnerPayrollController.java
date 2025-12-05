// src/main/java/com/erp/erp_back/controller/hr/OwnerPayrollController.java
package com.erp.erp_back.controller.hr;

import com.erp.erp_back.dto.hr.OwnerPayrollResponse;
import com.erp.erp_back.dto.hr.PayrollCalcResultDto;
import com.erp.erp_back.service.hr.OwnerPayrollService;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner/payroll")
public class OwnerPayrollController {

    private final OwnerPayrollService ownerPayrollService;

    public OwnerPayrollController(OwnerPayrollService ownerPayrollService) {
        this.ownerPayrollService = ownerPayrollService;
    }

    // ✅ 기존: 이번 달 급여 조회
    @GetMapping
    public ResponseEntity<OwnerPayrollResponse> getMonthlyPayroll(
        @RequestParam Long storeId,
        @RequestParam String yearMonth
    ) {
        YearMonth ym = YearMonth.parse(yearMonth); // "2024-04"
        return ResponseEntity.ok(ownerPayrollService.getMonthlyPayroll(storeId, ym));
    }

    // ✅ 신규: 급여 자동 계산 (프론트에서 POST /owner/payroll/calc 호출)
    @PostMapping("/calc")
    public ResponseEntity<PayrollCalcResultDto> calculatePayroll(
        @RequestParam Long storeId,
        @RequestParam String yearMonth
    ) {
        YearMonth ym = YearMonth.parse(yearMonth);
        PayrollCalcResultDto result = ownerPayrollService.calculateMonthlyPayroll(storeId, ym);
        return ResponseEntity.ok(result);
    }
}