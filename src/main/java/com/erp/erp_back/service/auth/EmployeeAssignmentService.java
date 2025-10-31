package com.erp.erp_back.service.auth;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.auth.EmployeeAssignmentRequest;
import com.erp.erp_back.dto.auth.EmployeeAssignmentResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeAssignmentService {

    private final EmployeeAssignmentRepository assignmentRepo;
    private final EmployeeRepository employeeRepo;
    private final StoreRepository storeRepo;

    /**
     * 직원이 사업장 코드(storeId)로 근무 신청
     * - 중복(PENDING/APPROVED) 신청 방지
     * - 기본 상태는 PENDING
     */
    public EmployeeAssignmentResponse apply(EmployeeAssignmentRequest req) {
        // 직원/매장 존재 확인
        Employee emp = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));
        Store store = storeRepo.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        // 중복 신청/승인 방지
        assignmentRepo.findFirstByEmployee_EmployeeIdAndStore_StoreIdAndStatusIn(
                emp.getEmployeeId(), store.getStoreId(), List.of("PENDING", "APPROVED")
        ).ifPresent(a -> {
            throw new IllegalStateException("이미 신청 중이거나 승인된 상태입니다.");
        });

        // 저장
        EmployeeAssignment saved = new EmployeeAssignment();
        saved.setEmployee(emp);
        saved.setStore(store);
        saved.setRole(req.getRole());
        if (saved.getStatus() == null || saved.getStatus().isBlank()) {
            saved.setStatus("PENDING"); // 엔티티에 기본값 없다면 명시
        }

        saved = assignmentRepo.save(saved);

        return EmployeeAssignmentResponse.builder()
                .assignmentId(saved.getAssignmentId())
                .employeeId(emp.getEmployeeId())
                .storeId(store.getStoreId())
                .role(saved.getRole())
                .status(saved.getStatus())
                .build();
    }

    /**
     * 사장 측: 특정 사업장(storeId)의 '신청대기' 직원 목록
     */
    @Transactional(readOnly = true)
    public List<EmployeeAssignmentResponse> listPendingByStore(Long storeId) {
        // 존재하는 사업장인지 검증
        storeRepo.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        List<EmployeeAssignment> rows = assignmentRepo.findPendingByStoreId(storeId);

        return rows.stream()
                .map(a -> EmployeeAssignmentResponse.builder()
                        .assignmentId(a.getAssignmentId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .storeId(a.getStore().getStoreId())
                        .role(a.getRole())
                        .status(a.getStatus())
                        .build())
                .toList();
    }

    /**
     * 사장 측: 신청 승인
     */
    public EmployeeAssignmentResponse approve(Long assignmentId) {
        EmployeeAssignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
        if (!"PENDING".equals(a.getStatus())) {
            throw new IllegalStateException("대기 상태가 아닌 신청은 승인할 수 없습니다.");
        }
        a.setStatus("APPROVED");
        // 필요 시 직원-사업장 실제 연결 로직 추가

        return EmployeeAssignmentResponse.builder()
                .assignmentId(a.getAssignmentId())
                .employeeId(a.getEmployee().getEmployeeId())
                .storeId(a.getStore().getStoreId())
                .role(a.getRole())
                .status(a.getStatus())
                .build();
    }

    /**
     * 사장 측: 신청 거절
     */
    public EmployeeAssignmentResponse reject(Long assignmentId) {
        EmployeeAssignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
        if (!"PENDING".equals(a.getStatus())) {
            throw new IllegalStateException("대기 상태가 아닌 신청은 거절할 수 없습니다.");
        }
        a.setStatus("REJECTED");

        return EmployeeAssignmentResponse.builder()
                .assignmentId(a.getAssignmentId())
                .employeeId(a.getEmployee().getEmployeeId())
                .storeId(a.getStore().getStoreId())
                .role(a.getRole())
                .status(a.getStatus())
                .build();
    }
}