package com.erp.erp_back.controller.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.store.StoreSimpleResponse; // ✅ DTO Import 추가
import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.service.user.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /** ✅ 해당 사업장에 승인된 직원 목록만 조회 */
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list(@RequestParam Long storeId) {
        return ResponseEntity.ok(employeeService.getEmployeesByStore(storeId));
    }

    /** 직원 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    /** ✅ [여기 추가] 해당 직원이 소속된 매장 목록 조회 (드롭다운용) */
    @GetMapping("/{id}/stores")
    public ResponseEntity<List<StoreSimpleResponse>> getStores(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getStoresByEmployee(id));
    }

    /** 직원 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}