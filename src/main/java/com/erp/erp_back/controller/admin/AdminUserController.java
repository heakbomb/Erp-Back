package com.erp.erp_back.controller.admin;

import java.util.List; // ⭐️ 추가

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.dto.user.OwnerResponse;
import com.erp.erp_back.dto.admin.AdminStoreDashboardItem; // ⭐️ 추가
import com.erp.erp_back.dto.admin.AdminUserStatsResponse; // ⭐️ 추가
import com.erp.erp_back.service.user.EmployeeService;
import com.erp.erp_back.service.user.OwnerService;
import com.erp.erp_back.service.admin.AdminUserService; // ⭐️ 추가

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users") // ✅ 관리자용 사용자 API 루트
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // (프론트 주소 확인)
public class AdminUserController {

    private final OwnerService ownerService;
    private final EmployeeService employeeService;
    private final AdminUserService adminUserService; // ⭐️ (신규) AdminUserService 주입

    /**
     * ⭐️ (신규) (Admin) 사용자 통계 KPI 조회
     * GET /admin/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminUserStatsResponse> getAdminUserStats() {
        AdminUserStatsResponse stats = adminUserService.getAdminUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * (Admin) '사장님' 계정 목록 조회 (페이징 및 검색)
     * GET /admin/users/owners?q=test@&page=0&size=10
     */
    @GetMapping("/owners")
    public ResponseEntity<Page<OwnerResponse>> getOwners(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "ownerId") Pageable pageable
    ) {
        Page<OwnerResponse> ownerPage = ownerService.getOwnersForAdmin(q, pageable);
        return ResponseEntity.ok(ownerPage);
    }

    /**
     * ⭐️ (신규) (Admin) '사장님' 1명 상세 조회
     * GET /admin/users/owners/{id}
     */
    @GetMapping("/owners/{id}")
    public ResponseEntity<OwnerResponse> getOwnerDetails(@PathVariable Long id) {
        OwnerResponse owner = ownerService.getOwnerById(id);
        return ResponseEntity.ok(owner);
    }

    /**
     * ⭐️ (신규) (Admin) 특정 사장님의 사업장 대시보드 조회
     * GET /admin/users/owners/{id}/stores-dashboard
     */
    @GetMapping("/owners/{id}/stores-dashboard")
    public ResponseEntity<List<AdminStoreDashboardItem>> getOwnerStoreDashboard(@PathVariable Long id) {
        List<AdminStoreDashboardItem> dashboard = adminUserService.getOwnerStoreDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * (Admin) '직원' 계정 목록 조회 (페이징 및 검색)
     * GET /admin/users/employees?q=test@&page=0&size=10
     */
    @GetMapping("/employees")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "employeeId") Pageable pageable
    ) {
        Page<EmployeeResponse> employeePage = employeeService.getEmployeesForAdmin(q, pageable);
        return ResponseEntity.ok(employeePage);
    }

    // --- (AdminStoreController와 동일한 예외 핸들러) ---
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}