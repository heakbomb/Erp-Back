// src/main/java/com/erp/erp_back/controller/hr/EmployeePayrollController.java
package com.erp.erp_back.controller.hr;

import com.erp.erp_back.dto.hr.PayrollHistoryDetailDto;
import com.erp.erp_back.service.hr.PayrollHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee/payroll")
@RequiredArgsConstructor
public class EmployeePayrollController {

    private final PayrollHistoryService payrollHistoryService;

    /**
     * ✅ 직원 본인의 급여 이력 조회
     *
     *  - GET  /employee/payroll/history?storeId=1&employeeId=10
     *  - 응답: List<PayrollHistoryDetailDto>
     */
    @GetMapping("/history")
    public ResponseEntity<List<PayrollHistoryDetailDto>> getEmployeeHistory(
            @RequestParam Long storeId,
            @RequestParam Long employeeId
    ) {
        List<PayrollHistoryDetailDto> result =
                payrollHistoryService.getEmployeeHistory(storeId, employeeId);

        return ResponseEntity.ok(result);
    }
}