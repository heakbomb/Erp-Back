// src/main/java/com/erp/erp_back/service/user/EmployeeService.java
package com.erp.erp_back.service.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.EmployeeMapper;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;

    /** (Admin) 직원 계정 페이징 및 검색 조회 */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployeesForAdmin(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();
        return employeeRepository.findAdminEmployees(effectiveQuery, pageable)
                .map(employeeMapper::toResponse);
    }

    /** 직원 전체 목록 조회 (다른 곳에서 쓰면 사용) */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    /** ✅ 특정 사업장에 등록된 *승인된* 직원 목록 조회 */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByStore(Long storeId) {

        // 1) 해당 매장의 APPROVED 배정 정보
        List<EmployeeAssignment> assignments =
                employeeAssignmentRepository.findApprovedByStoreId(storeId);

        // 2) 배정 + 직원 정보를 EmployeeResponse 로 수동 매핑
        return assignments.stream()
                .map(a -> {
                    Employee e = a.getEmployee();
                    return EmployeeResponse.builder()
                            .employeeId(e.getEmployeeId())
                            .assignmentId(a.getAssignmentId())   // 필요하면 프론트에서 사용
                            .name(e.getName())
                            .email(e.getEmail())
                            .phone(e.getPhone())
                            .provider(e.getProvider())
                            .createdAt(e.getCreatedAt())
                            .build();
                })
                .toList();
    }

    /** 단일 직원 조회 */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다."));
        return employeeMapper.toResponse(emp);
    }

    /**
     * ✅ 직원-사업장 배정 해제(퇴사 처리)
     * - employee 테이블은 건드리지 않고
     * - employee_assignment.status를 ENDED로 변경
     * - 이력은 그대로 남는다.
     */
    public void endAssignment(Long assignmentId) {
        EmployeeAssignment a = employeeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배정(assignmentId=" + assignmentId + ")을 찾을 수 없습니다."));

        // 이미 APPROVED 상태에서만 목록에 뜨므로,
        // 여기서 ENDED로 바꾸면 이후 getEmployeesByStore()에서 자동으로 안 보이게 됨.
        a.setStatus("ENDED");
    }
}