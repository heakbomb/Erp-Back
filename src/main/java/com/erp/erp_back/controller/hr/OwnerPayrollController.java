// src/main/java/com/erp/erp_back/controller/hr/OwnerPayrollController.java
package com.erp.erp_back.controller.hr;

import com.erp.erp_back.dto.hr.OwnerPayrollResponse;
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

    // GET /owner/payroll?storeId=1&yearMonth=2024-04
    @GetMapping
    public ResponseEntity<OwnerPayrollResponse> getMonthlyPayroll(
        @RequestParam Long storeId,
        @RequestParam String yearMonth
    ) {
        YearMonth ym = YearMonth.parse(yearMonth); // "2024-04"
        return ResponseEntity.ok(ownerPayrollService.getMonthlyPayroll(storeId, ym));
    }
}