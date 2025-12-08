// src/main/java/com/erp/erp_back/controller/hr/PayrollSettingController.java
package com.erp.erp_back.controller.hr;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.hr.PayrollSettingDto;
import com.erp.erp_back.service.hr.PayrollSettingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/payroll/settings")
@RequiredArgsConstructor
public class PayrollSettingController {

    private final PayrollSettingService payrollSettingService;

    /** ✅ 매장별 급여 설정 조회 */
    @GetMapping
    public ResponseEntity<List<PayrollSettingDto>> list(@RequestParam Long storeId) {
        return ResponseEntity.ok(payrollSettingService.getSettingsByStore(storeId));
    }

    /** ✅ 단일 직원 설정 저장/수정 */
    @PutMapping("/{employeeId}")
    public ResponseEntity<PayrollSettingDto> upsert(
            @RequestParam Long storeId,
            @PathVariable Long employeeId,
            @RequestBody PayrollSettingDto body
    ) {
        body.setEmployeeId(employeeId);
        return ResponseEntity.ok(payrollSettingService.saveSetting(storeId, employeeId, body));
    }
}