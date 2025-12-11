package com.erp.erp_back.service.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.store.StoreSimpleResponse; // ✅ DTO Import 추가
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
        List<EmployeeAssignment> assignments =
                employeeAssignmentRepository.findApprovedByStoreId(storeId);

        return assignments.stream()
                .map(a -> {
                    Employee e = a.getEmployee();
                    return EmployeeResponse.builder()
                            .employeeId(e.getEmployeeId())
                            .assignmentId(a.getAssignmentId())
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

    /** ✅ [여기 추가] 특정 직원이 소속된 매장 목록 조회 */
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByEmployee(Long employeeId) {
        return employeeAssignmentRepository.findAllByEmployeeId(employeeId).stream()
                .map(ea -> StoreSimpleResponse.builder()
                        .storeId(ea.getStore().getStoreId())
                        .storeName(ea.getStore().getStoreName())
                        .industry(ea.getStore().getIndustry())
                        .status(ea.getStore().getStatus())
                        .posVendor(ea.getStore().getPosVendor())
                        .bizNum(ea.getStore().getBusinessNumber().getBizNum())
                        .build())
                .toList();
    }

    /** 직원 삭제 */
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다.");
        }
        employeeRepository.deleteById(id);
    }
}