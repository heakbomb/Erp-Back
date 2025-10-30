package com.erp.erp_back.service.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.user.EmployeeResponse;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /** 직원 전체 목록 조회 */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /** 단일 직원 조회 */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다."));
        return toDto(emp);
    }

    /** 직원 정보 수정 */
    public EmployeeResponse updateEmployee(Long id, EmployeeResponse req) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다."));

        // 수정 가능한 필드 업데이트
        emp.setName(req.getName());
        emp.setEmail(req.getEmail());
        emp.setPhone(req.getPhone());
        emp.setProvider(req.getProvider()); // 항상 소셜 로그인용
        // providerId는 일반적으로 변경 불가하므로 제외 (필요 시 추가)

        Employee updated = employeeRepository.save(emp);
        return toDto(updated);
    }

    /** 직원 삭제 */
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 직원(ID=" + id + ")을 찾을 수 없습니다.");
        }
        employeeRepository.deleteById(id);
    }

    /** Entity → DTO 변환 */
    private EmployeeResponse toDto(Employee e) {
        return EmployeeResponse.builder()
                .employeeId(e.getEmployeeId())
                .name(e.getName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .provider(e.getProvider())
                .createdAt(e.getCreatedAt())
                .build();
    }
}