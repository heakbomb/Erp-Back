package com.erp.erp_back.controller.auth;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.auth.EmployeeAssignmentRequest;
import com.erp.erp_back.dto.auth.EmployeeAssignmentResponse;
import com.erp.erp_back.service.auth.EmployeeAssignmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class EmployeeAssignmentController {

    private final EmployeeAssignmentService service;

    /** 직원 신청 */
    @PostMapping("/apply")
    public ResponseEntity<EmployeeAssignmentResponse> apply(@Validated @RequestBody EmployeeAssignmentRequest req) {
        return ResponseEntity.ok(service.apply(req));
    }

    /** 특정 사업장별 신청 대기 목록 */
    @GetMapping("/pending")
    public ResponseEntity<List<EmployeeAssignmentResponse>> pendingByStore(@RequestParam Long storeId) {
        return ResponseEntity.ok(service.listPendingByStore(storeId));
    }

    /** ✅ 신청 승인 */
    @PostMapping("/{assignmentId}/approve")
    public ResponseEntity<EmployeeAssignmentResponse> approve(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(service.approve(assignmentId));
    }

    /** ✅ 신청 거절 */
    @PostMapping("/{assignmentId}/reject")
    public ResponseEntity<EmployeeAssignmentResponse> reject(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(service.reject(assignmentId));
    }

    // 에러 메시지 정리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }
}