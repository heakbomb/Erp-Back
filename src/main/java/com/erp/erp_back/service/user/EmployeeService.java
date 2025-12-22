package com.erp.erp_back.service.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.user.Employee;
// StoreIndustry 임포트 불필요 (Mapper가 처리함)
import com.erp.erp_back.mapper.EmployeeMapper;
import com.erp.erp_back.mapper.StoreMapper; // ✅ 추가
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
    private final StoreMapper storeMapper; // ✅ StoreMapper 주입

    /** (Admin) 직원 계정 페이징 및 검색 조회 */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployeesForAdmin(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();
        return employeeRepository.findAdminEmployees(effectiveQuery, pageable)
                .map(employeeMapper::toResponse);
    }

    /** 직원 전체 목록 조회 */
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

    /**
     * ✅ 직원-사업장 배정 해제(퇴사 처리)
     */
    public void endAssignment(Long assignmentId) {
        EmployeeAssignment a = employeeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배정(assignmentId=" + assignmentId + ")을 찾을 수 없습니다."));
        a.setStatus("ENDED");
    }

    /** ✅ [수정됨] 특정 직원이 소속된 매장 목록 조회 (Mapper 사용) */
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByEmployee(Long employeeId) {
        return employeeAssignmentRepository.findAllByEmployeeId(employeeId).stream()
                // ✅ 수동 Builder 대신 StoreMapper 사용
                // MapStruct가 String(Entity) <-> Enum(DTO) 변환을 자동으로 처리해줍니다.
                .map(ea -> storeMapper.toSimpleResponse(ea.getStore())) 
                .toList();
    }
}