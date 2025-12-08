package com.erp.erp_back.service.hr;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.PayrollCalcResultDto;
import com.erp.erp_back.dto.hr.PayrollHistoryDetailDto;
import com.erp.erp_back.dto.hr.PayrollHistoryDto;
import com.erp.erp_back.dto.hr.OwnerPayrollResponse.EmployeePayroll;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.hr.PayrollHistory;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.hr.PayrollHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollHistoryService {

    private final PayrollHistoryRepository payrollHistoryRepository;
    private final OwnerPayrollService ownerPayrollService;
    private final EmployeeAssignmentRepository employeeAssignmentRepository; // ✅ 역할 조회용

    /**
     * ✅ 1) 특정 매장 + 특정 월의 급여를 계산하고
     *      2) payroll_history 테이블에 직원별로 저장/업데이트(upsert) 한다.
     *
     *  - 호출 시점: "급여 자동 계산"을 확정해서 지급 내역에 반영하고 싶을 때 사용
     */
    public List<PayrollHistoryDetailDto> saveMonthlyHistory(Long storeId, YearMonth yearMonth) {

        // 1) 기존 계산 로직 재사용 (이미 공제/실수령액까지 계산되어 있음)
        PayrollCalcResultDto calcResult =
                ownerPayrollService.calculateMonthlyPayroll(storeId, yearMonth);

        String ym = yearMonth.toString(); // "2025-12"

        // Store / Employee 참조용 얕은 객체
        Store storeRef = new Store();
        storeRef.setStoreId(storeId);

        List<PayrollHistoryDetailDto> result = new ArrayList<>();

        for (EmployeePayroll emp : calcResult.getEmployees()) {

            // 2) 기존 레코드가 있으면 가져오고, 없으면 새로 생성
            PayrollHistory entity =
                    payrollHistoryRepository
                            .findByStore_StoreIdAndEmployee_EmployeeIdAndPayrollMonth(
                                    storeId, emp.getId(), ym
                            )
                            .orElseGet(() -> {
                                PayrollHistory ph = new PayrollHistory();

                                ph.setStore(storeRef);

                                Employee eRef = new Employee();
                                eRef.setEmployeeId(emp.getId());
                                ph.setEmployee(eRef);

                                ph.setPayrollMonth(ym);
                                ph.setStatus("PENDING"); // 처음 저장 시에는 예정 상태

                                return ph;
                            });

            // 3) 값 매핑
            long workMinutes = Math.round(emp.getWorkHours() * 60.0);
            long grossPay = emp.getNetPay() + emp.getDeductions();

            entity.setWorkDays(emp.getWorkDays());
            entity.setWorkMinutes(workMinutes);
            entity.setGrossPay(grossPay);
            entity.setDeductions(emp.getDeductions());
            entity.setNetPay(emp.getNetPay());

            entity.setBaseWage(emp.getBasePay());
            // wageType은 OwnerPayrollResponse.EmployeePayroll 에 아직 없으니 필요하면 나중에 확장
            entity.setWageType(emp.getWageType()); 

            entity.setDeductionType(emp.getDeductionType());

            // 4) 저장
            PayrollHistory saved = payrollHistoryRepository.save(entity);

            // 5) DTO 변환 (직원 이름/역할은 계산 결과에서 그대로 사용)
            PayrollHistoryDetailDto dto = PayrollHistoryDetailDto.builder()
                    .payrollId(saved.getPayrollId())
                    .employeeId(emp.getId())
                    .employeeName(emp.getName())
                    .role(emp.getRole())
                    .yearMonth(ym)
                    .workDays(saved.getWorkDays())
                    .workMinutes(saved.getWorkMinutes())
                    .grossPay(saved.getGrossPay())
                    .deductions(saved.getDeductions())
                    .netPay(saved.getNetPay())
                    .wageType(saved.getWageType())
                    .baseWage(saved.getBaseWage())
                    .deductionType(saved.getDeductionType())
                    .status(saved.getStatus())
                    .paidAt(saved.getPaidAt())
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * ✅ 급여 지급 상태 변경 (예정 ↔ 지급완료)
     *  - 프론트에서 status는 "PENDING"/"PAID" 또는 "예정"/"지급완료" 둘 다 보낼 수 있음
     *  - status = PAID 가 되면 paidAt 에 지금 시간을 넣고,
     *    다시 PENDING 으로 돌리면 paidAt 을 null 로 초기화.
     */
    public PayrollHistoryDetailDto updateStatus(Long payrollId, String rawStatus) {

        PayrollHistory entity = payrollHistoryRepository.findById(payrollId)
                .orElseThrow(() -> new IllegalArgumentException("급여 내역을 찾을 수 없습니다. id=" + payrollId));

        // 한글/영문 둘 다 허용
        String normalized = normalizeStatus(rawStatus);

        entity.setStatus(normalized);

        if ("PAID".equals(normalized)) {
            entity.setPaidAt(LocalDateTime.now());
        } else if ("PENDING".equals(normalized)) {
            entity.setPaidAt(null);
        }

        PayrollHistory saved = payrollHistoryRepository.save(entity);

        // ✅ 역할 조회 (해당 매장 + 직원)
        Long storeId = saved.getStore().getStoreId();
        Long empId = saved.getEmployee().getEmployeeId();

        String role = employeeAssignmentRepository
                .findByStore_StoreIdAndEmployee_EmployeeId(storeId, empId)
                .map(EmployeeAssignment::getRole)
                .orElse(null);

        // ✅ 기존 getMonthlyHistory 에서 사용하던 DTO 매핑 로직과 동일하게 구성
        return PayrollHistoryDetailDto.builder()
                .payrollId(saved.getPayrollId())
                .employeeId(empId)
                .employeeName(saved.getEmployee().getName())
                .role(role)
                .yearMonth(saved.getPayrollMonth())
                .workDays(saved.getWorkDays())
                .workMinutes(saved.getWorkMinutes())
                .grossPay(saved.getGrossPay())
                .deductions(saved.getDeductions())
                .netPay(saved.getNetPay())
                .wageType(saved.getWageType())
                .baseWage(saved.getBaseWage())
                .deductionType(saved.getDeductionType())
                .status(saved.getStatus())
                .paidAt(saved.getPaidAt())
                .build();
    }

    /**
     * ✅ 상태 문자열 정규화
     *   - 프론트에서 "예정"/"지급완료"를 보내도 내부에서는 PENDING/PAID 로 맞춤
     */
    private String normalizeStatus(String raw) {
        if (raw == null) return "PENDING";

        String trimmed = raw.trim();

        // 한글 우선
        if ("지급완료".equals(trimmed)) return "PAID";
        if ("예정".equals(trimmed)) return "PENDING";

        // 영문 (대소문자 무시)
        String upper = trimmed.toUpperCase();
        if ("PAID".equals(upper)) return "PAID";
        if ("PENDING".equals(upper)) return "PENDING";

        // 모르면 기본값은 PENDING
        return "PENDING";
    }

    /**
     * ✅ 특정 매장 + 특정 월의 급여 지급 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PayrollHistoryDetailDto> getMonthlyHistory(Long storeId, YearMonth yearMonth) {
        String ym = yearMonth.toString();

        List<PayrollHistory> list =
                payrollHistoryRepository
                        .findByStore_StoreIdAndPayrollMonthOrderByEmployee_EmployeeIdAsc(
                                storeId, ym
                        );

        // ✅ 이 매장의 직원-역할 매핑을 한 번에 가져오기
        List<EmployeeAssignment> assignments =
                employeeAssignmentRepository.findByStore_StoreId(storeId);

        Map<Long, String> roleMap = assignments.stream()
                .collect(Collectors.toMap(
                        a -> a.getEmployee().getEmployeeId(),
                        EmployeeAssignment::getRole,
                        (r1, r2) -> r1 // 중복 있을 경우 첫 번째 값 사용
                ));

        List<PayrollHistoryDetailDto> result = new ArrayList<>();

        for (PayrollHistory h : list) {
            Long empId = h.getEmployee().getEmployeeId();
            String role = roleMap.get(empId);

            PayrollHistoryDetailDto dto = PayrollHistoryDetailDto.builder()
                    .payrollId(h.getPayrollId())
                    .employeeId(empId)
                    .employeeName(h.getEmployee().getName())  // LAZY지만 트랜잭션 안에서라 OK
                    .role(role)
                    .yearMonth(h.getPayrollMonth())
                    .workDays(h.getWorkDays())
                    .workMinutes(h.getWorkMinutes())
                    .grossPay(h.getGrossPay())
                    .deductions(h.getDeductions())
                    .netPay(h.getNetPay())
                    .wageType(h.getWageType())
                    .baseWage(h.getBaseWage())
                    .deductionType(h.getDeductionType())
                    .status(h.getStatus())
                    .paidAt(h.getPaidAt())
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * ✅ 매장별 급여 지급 내역 요약 (월별)
     *
     *  - payroll_history 를 월별로 묶어서
     *    * month: "2025-12"
     *    * totalPaid: 해당 월 netPay 합계
     *    * employees: 해당 월에 급여가 있는 직원 수
     *    * status: 직원들 중 하나라도 PENDING 이면 "예정", 모두 PAID 이면 "완료"
     */
    @Transactional(readOnly = true)
    public List<PayrollHistoryDto> getHistorySummary(Long storeId) {

        // 1) 매장의 전체 history 를 월 내림차순으로 조회
        List<PayrollHistory> list =
                payrollHistoryRepository.findByStore_StoreIdOrderByPayrollMonthDesc(storeId);

        // 2) payrollMonth 기준으로 그룹핑 (LinkedHashMap 으로 순서 유지)
        Map<String, List<PayrollHistory>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        PayrollHistory::getPayrollMonth,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PayrollHistoryDto> result = new ArrayList<>();

        for (Map.Entry<String, List<PayrollHistory>> entry : grouped.entrySet()) {
            String ym = entry.getKey();                 // "2025-12"
            List<PayrollHistory> rows = entry.getValue();

            // 해당 월 총 실수령액 합계
            long totalPaid = rows.stream()
                    .mapToLong(PayrollHistory::getNetPay)
                    .sum();

            // 급여가 있는 직원 수(중복 제거)
            int employees = (int) rows.stream()
                    .map(ph -> ph.getEmployee().getEmployeeId())
                    .distinct()
                    .count();

            // 하나라도 PENDING 이면 "예정", 전부 PAID 면 "완료"
            boolean hasPending = rows.stream()
                    .anyMatch(ph -> "PENDING".equals(ph.getStatus()));

            String status = hasPending ? "예정" : "완료";

            result.add(PayrollHistoryDto.builder()
                    .month(ym)
                    .totalPaid(totalPaid)
                    .employees(employees)
                    .status(status)
                    .build());
        }

        return result;
    }
    /**
     * ✅ 특정 직원의 전체 급여 이력 조회 (직원 페이지용)
     *
     *  - storeId : 현재 선택된 사업장 ID
     *  - employeeId : 로그인한 직원 ID
     */
    @Transactional(readOnly = true)
    public List<PayrollHistoryDetailDto> getEmployeeHistory(Long storeId, Long employeeId) {

        // 1) 해당 매장 + 직원의 급여 이력 (최근 월 순)
        List<PayrollHistory> histories =
                payrollHistoryRepository.findByStore_StoreIdAndEmployee_EmployeeIdOrderByPayrollMonthDesc(
                        storeId,
                        employeeId
                );

        // 2) 변환 로직 재사용 (아래에 toDetailDto 를 구현)
        return histories.stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * 엔티티 -> DTO 변환 헬퍼
     */
    private PayrollHistoryDetailDto toDetailDto(PayrollHistory h) {
        Long storeId = h.getStore() != null ? h.getStore().getStoreId() : null;
        Long empId = h.getEmployee() != null ? h.getEmployee().getEmployeeId() : null;

        String role = null;
        if (storeId != null && empId != null) {
            role = employeeAssignmentRepository
                    .findByStore_StoreIdAndEmployee_EmployeeId(storeId, empId)
                    .map(EmployeeAssignment::getRole)
                    .orElse(null);
        }

        return PayrollHistoryDetailDto.builder()
                .payrollId(h.getPayrollId())
                .employeeId(empId)
                .employeeName(h.getEmployee() != null ? h.getEmployee().getName() : null)
                .role(role)
                .yearMonth(h.getPayrollMonth())
                .workDays(h.getWorkDays())
                .workMinutes(h.getWorkMinutes())
                .grossPay(h.getGrossPay())
                .deductions(h.getDeductions())
                .netPay(h.getNetPay())
                .wageType(h.getWageType())
                .baseWage(h.getBaseWage())
                .deductionType(h.getDeductionType())
                .status(h.getStatus())
                .paidAt(h.getPaidAt())
                .storeName(h.getStore() != null ? h.getStore().getStoreName() : null)
                .build();
    }
}