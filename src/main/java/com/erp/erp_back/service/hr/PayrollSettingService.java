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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollSettingService {

    private final PayrollSettingRepository payrollSettingRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final ObjectMapper objectMapper; // ✅ 공제 JSON 파싱용

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

            // ✅ 공제 정보 파싱
            String deductionType = "NONE";
            Double deductionRate = null;

            if (setting != null && setting.getDeductionItems() != null) {
                String json = setting.getDeductionItems();
                try {
                    JsonNode node = objectMapper.readTree(json);
                    if (node.hasNonNull("type")) {
                        deductionType = node.get("type").asText("NONE");
                    }
                    if (node.hasNonNull("rate")) {
                        deductionRate = node.get("rate").asDouble();
                    }
                } catch (Exception ignore) {
                    // 파싱 실패 시 기본값 유지
                }
            }

            PayrollSettingDto dto = PayrollSettingDto.builder()
                    .settingId(setting != null ? setting.getSettingId() : null)
                    .employeeId(emp.getEmployeeId())
                    .employeeName(emp.getName())
                    .role(assign.getRole())              // 🔹 역할도 같이 내려줌
                    .baseWage(baseWage)
                    .wageType(wageType)
                    .deductionType(deductionType)        // 🔹 공제 타입
                    .deductionRate(deductionRate)        // 🔹 공제율(있으면)
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

        // ✅ 공제 항목 JSON 저장
        String deductionType = dto.getDeductionType();
        Double deductionRate = dto.getDeductionRate();

        if (deductionType == null || deductionType.isBlank() || "NONE".equals(deductionType)) {
            // 공제 없음
            entity.setDeductionItems(null);
        } else {
            // {"type":"FOUR_INSURANCE","rate":0.033} 이런 형식으로 저장
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"").append(deductionType).append("\"");
            if (deductionRate != null) {
                sb.append(",\"rate\":").append(deductionRate);
            }
            sb.append("}");
            entity.setDeductionItems(sb.toString());
        }

        PayrollSetting saved = payrollSettingRepository.save(entity);

        // 3) 이름 / 역할 보정: 프론트에서 employeeName, role 을 안 보내 준 경우 DB에서 다시 찾기
        String employeeName = dto.getEmployeeName();
        String role = dto.getRole();

        if (employeeName == null || employeeName.isBlank() || role == null) {
            EmployeeAssignment assignment = employeeAssignmentRepository
                    .findApprovedByStoreId(storeId).stream()
                    .filter(a -> a.getEmployee() != null
                            && employeeId.equals(a.getEmployee().getEmployeeId()))
                    .findFirst()
                    .orElse(null);

            if (assignment != null && assignment.getEmployee() != null) {
                if (employeeName == null || employeeName.isBlank()) {
                    employeeName = assignment.getEmployee().getName();
                }
                if (role == null) {
                    role = assignment.getRole();
                }
            }
        }

        // 4) 다시 DTO 로 리턴
        return PayrollSettingDto.builder()
                .settingId(saved.getSettingId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .role(role)
                .baseWage(saved.getBaseWage() != null ? saved.getBaseWage().longValue() : 0L)
                .wageType(saved.getWageType())
                .deductionType(deductionType == null ? "NONE" : deductionType)
                .deductionRate(deductionRate)
                .build();
    }
}