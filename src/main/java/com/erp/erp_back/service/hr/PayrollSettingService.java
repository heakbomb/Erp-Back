// src/main/java/com/erp/erp_back/service/hr/PayrollSettingService.java
package com.erp.erp_back.service.hr;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.PayrollSettingDto;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.hr.PayrollSetting;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.hr.PayrollSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollSettingService {

    private final PayrollSettingRepository payrollSettingRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;

    /**
     * ✅ 특정 매장의 직원별 급여 설정 조회
     *  - 승인( APPROVED )된 직원 기준
     *  - 설정이 없으면 기본값(시급 0원, wageType = HOURLY)으로 내려줌
     */
    @Transactional(readOnly = true)
    public List<PayrollSettingDto> getSettingsByStore(Long storeId) {

        // 1) 이 매장에 승인된 직원 목록 (JOIN FETCH employee)
        List<EmployeeAssignment> assignments =
                employeeAssignmentRepository.findApprovedByStoreId(storeId);

        // 2) 기존 급여 설정들을 employeeId 기준으로 맵핑
        Map<Long, PayrollSetting> settingMap =
                payrollSettingRepository.findAllByStore_StoreId(storeId).stream()
                        .collect(Collectors.toMap(
                                ps -> ps.getEmployee().getEmployeeId(),
                                Function.identity()
                        ));

        // 3) 직원 + 설정 조합해서 DTO 리스트 생성
        List<PayrollSettingDto> result = new ArrayList<>();

        for (EmployeeAssignment assign : assignments) {
            Employee emp = assign.getEmployee();
            PayrollSetting setting = settingMap.get(emp.getEmployeeId());

            long baseWage =
                    (setting != null && setting.getBaseWage() != null)
                            ? setting.getBaseWage().longValue()
                            : 0L;

            String wageType =
                    (setting != null && setting.getWageType() != null)
                            ? setting.getWageType()
                            : "HOURLY";

            PayrollSettingDto dto = PayrollSettingDto.builder()
                    .settingId(setting != null ? setting.getSettingId() : null)
                    .employeeId(emp.getEmployeeId())
                    .employeeName(emp.getName())
                    .baseWage(baseWage)
                    .wageType(wageType)
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * ✅ 직원 한 명의 급여 설정 저장 / 수정(Upsert)
     */
    public PayrollSettingDto saveSetting(Long storeId, Long employeeId, PayrollSettingDto dto) {

        // 1) 기존 설정 있으면 가져오고, 없으면 새로 생성
        PayrollSetting entity = payrollSettingRepository
                .findByStore_StoreIdAndEmployee_EmployeeId(storeId, employeeId)
                .orElseGet(() -> {
                    PayrollSetting ps = new PayrollSetting();

                    Store storeRef = new Store();
                    storeRef.setStoreId(storeId);
                    ps.setStore(storeRef);

                    Employee empRef = new Employee();
                    empRef.setEmployeeId(employeeId);
                    ps.setEmployee(empRef);

                    return ps;
                });

        // 2) 값 업데이트
        //    DTO는 baseWage = Long, 엔티티는 BigDecimal 이라서 변환 필요
        if (dto.getBaseWage() != null) {
            entity.setBaseWage(BigDecimal.valueOf(dto.getBaseWage()));
        } else {
            entity.setBaseWage(BigDecimal.ZERO);
        }

        entity.setWageType(dto.getWageType() != null ? dto.getWageType() : "HOURLY");
        // ⚠ 현재 DTO에 deductionItems 필드가 없으므로 건들지 않음

        PayrollSetting saved = payrollSettingRepository.save(entity);

        // 3) 이름 보정: 프론트에서 employeeName 을 안 보내 준 경우 DB에서 다시 찾기
        String employeeName = dto.getEmployeeName();
        if (employeeName == null || employeeName.isBlank()) {
            EmployeeAssignment assignment = employeeAssignmentRepository
                    .findApprovedByStoreId(storeId).stream()
                    .filter(a -> a.getEmployee() != null
                            && employeeId.equals(a.getEmployee().getEmployeeId()))
                    .findFirst()
                    .orElse(null);

            if (assignment != null && assignment.getEmployee() != null) {
                employeeName = assignment.getEmployee().getName();
            }
        }

        // 4) 다시 DTO 로 리턴
        return PayrollSettingDto.builder()
                .settingId(saved.getSettingId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .baseWage(saved.getBaseWage() != null ? saved.getBaseWage().longValue() : 0L)
                .wageType(saved.getWageType())
                .build();
    }
}