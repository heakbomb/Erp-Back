package com.erp.erp_back.service.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.annotation.LogAudit;
import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.EmployeeMapper;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

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

    /** 단일 직원 조회 */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다."));
        return employeeMapper.toResponse(emp);
    }

    /** 직원 정보 수정 */
    public EmployeeResponse updateEmployee(Long id, EmployeeResponse req) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다."));

        employeeMapper.updateFromDto(req, emp);

        Employee updated = employeeRepository.save(emp);
        return employeeMapper.toResponse(updated);
    }

    /** 직원 삭제 */
    @LogAudit(action = "EMPLOYEE_DELETE", target = "Employee")
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다.");
        }
        employeeRepository.deleteById(id);
    }
}