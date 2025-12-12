package com.erp.erp_back.service.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.auth.EmployeeAssignmentRequest;
import com.erp.erp_back.dto.auth.EmployeeAssignmentResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.EmployeeAssignmentMapper;
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
    private final EmployeeAssignmentMapper assignmentMapper;

    /**
     * 직원이 사업장 코드(storeId)로 근무 신청
     * - 가장 최근 신청이 PENDING/APPROVED 면 재신청 불가
     * - 가장 최근 신청이 REJECTED 면 -> 기존 행을 PENDING 으로 되돌려 재사용
     * - 기본 상태는 PENDING
     */
    public EmployeeAssignmentResponse apply(EmployeeAssignmentRequest req) {
        // 직원/매장 존재 확인
        Employee emp = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));
        Store store = storeRepo.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        // ✅ 가장 최근 신청 1건 조회
        Optional<EmployeeAssignment> latestOpt =
                assignmentRepo.findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByAssignmentIdDesc(
                        emp.getEmployeeId(),
                        store.getStoreId()
                );

        if (latestOpt.isPresent()) {
            EmployeeAssignment latest = latestOpt.get();
            String status = latest.getStatus();

            // 아직 대기/승인 상태면 재신청 막기
            if ("PENDING".equals(status) || "APPROVED".equals(status)) {
                throw new IllegalStateException("이미 신청 중이거나 승인된 상태입니다.");
            }

            // ❗ REJECTED 인 경우: 새로 INSERT 하면 유니크 제약에 걸리므로
            //    기존 행의 status 만 PENDING 으로 되돌려서 재사용한다.
            if ("REJECTED".equals(status)) {
                latest.setStatus("PENDING");
                latest.setRole(req.getRole()); // 역할 다시 설정(변경 가능성 반영)

                // @Transactional 이라 Dirty Checking 으로 자동 UPDATE 됨
                return assignmentMapper.toResponse(latest);
            }
        }

        // 위에 해당 안 되면 (기존 이력이 없거나, 다른 상태 정책) 새 행 생성
        EmployeeAssignment saved = assignmentMapper.toEntity(req, emp, store);
        saved = assignmentRepo.save(saved);
        return assignmentMapper.toResponse(saved);
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
                .map(assignmentMapper::toResponse)
                .toList();
    }

    /**
     * 사장 측: 신청 승인
     * - Dirty Checking으로 자동 UPDATE
     */
    public EmployeeAssignmentResponse approve(Long assignmentId) {
        EmployeeAssignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

        if (!"PENDING".equals(a.getStatus())) {
            throw new IllegalStateException("대기 상태가 아닌 신청은 승인할 수 없습니다.");
        }

        a.setStatus("APPROVED");

        return assignmentMapper.toResponse(a);
    }

    /**
     * 사장 측: 신청 거절
     * - Dirty Checking으로 자동 UPDATE
     */
    public EmployeeAssignmentResponse reject(Long assignmentId) {
        EmployeeAssignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

        if (!"PENDING".equals(a.getStatus())) {
            throw new IllegalStateException("대기 상태가 아닌 신청은 거절할 수 없습니다.");
        }

        a.setStatus("REJECTED");

        return assignmentMapper.toResponse(a);
    }

    // ⭐️ 직원 + 사업장 기준 최신 신청 상태 조회 (없으면 Optional.empty)
    @Transactional(readOnly = true)
    public Optional<EmployeeAssignmentResponse> getLatestStatus(Long employeeId, Long storeId) {
        return assignmentRepo
                .findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByAssignmentIdDesc(employeeId, storeId)
                .map(assignmentMapper::toResponse);
    }
}