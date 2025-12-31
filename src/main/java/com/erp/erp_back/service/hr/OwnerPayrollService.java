// src/main/java/com/erp/erp_back/service/hr/OwnerPayrollService.java
package com.erp.erp_back.service.hr;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.OwnerPayrollResponse;
import com.erp.erp_back.dto.hr.OwnerPayrollResponse.EmployeePayroll;
import com.erp.erp_back.dto.hr.PayrollCalcResultDto;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.hr.PayrollSetting;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.hr.PayrollSettingRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerPayrollService {

    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final PayrollSettingRepository payrollSettingRepository;  // ✅ 급여설정 리포지토리
    private final ObjectMapper objectMapper;                          // ✅ 공제 JSON 파싱용

    /**
     * 이번 달 급여 화면용 기본 데이터 조회 + 공제/실수령액 계산
     */
    @Transactional(readOnly = true)
    public OwnerPayrollResponse getMonthlyPayroll(Long storeId, YearMonth yearMonth) {

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);

        // 1) 이 매장에 배정된 전체 직원 (필요하면 나중에 status=APPROVED 조건 추가)
        List<EmployeeAssignment> assignments =
            employeeAssignmentRepository.findByStore_StoreIdAndStatus(storeId, "APPROVED");

        // 1-1) 이 매장의 급여 설정들을 employeeId 기준으로 맵핑
        Map<Long, PayrollSetting> settingMap =
            payrollSettingRepository.findAllByStore_StoreId(storeId).stream()
                .collect(Collectors.toMap(
                    ps -> ps.getEmployee().getEmployeeId(),
                    Function.identity()
                ));

        // 2) 해당 기간의 전체 출결 로그
        List<AttendanceLog> logs =
            attendanceLogRepository.findByStoreAndDateTimeRange(storeId, from, to);

        // 2-1) 직원별로 로그 그룹핑
        Map<Long, List<AttendanceLog>> logsByEmp = new HashMap<>();
        for (AttendanceLog log : logs) {
            if (log.getEmployee() == null) continue;
            Long empId = log.getEmployee().getEmployeeId();
            logsByEmp.computeIfAbsent(empId, k -> new ArrayList<>()).add(log);
        }

        // 2-2) 직원별 근무일수 / 근무시간(분) 계산
        Map<Long, Long> workDaysMap = new HashMap<>();    // employeeId -> 근무일수
        Map<Long, Long> workMinutesMap = new HashMap<>(); // employeeId -> 근무시간(분)

        for (Map.Entry<Long, List<AttendanceLog>> entry : logsByEmp.entrySet()) {
            Long empId = entry.getKey();
            List<AttendanceLog> empLogs = entry.getValue();

            // 시간 순 정렬
            empLogs.sort(Comparator.comparing(AttendanceLog::getRecordTime));

            Set<LocalDate> days = new HashSet<>();
            long totalMinutes = 0L;
            LocalDateTime lastIn = null;

            for (AttendanceLog l : empLogs) {
                LocalDateTime time = l.getRecordTime();
                if (time == null) continue;

                days.add(time.toLocalDate());

                String type = l.getRecordType();
                if ("IN".equalsIgnoreCase(type)) {
                    lastIn = time;
                } else if ("OUT".equalsIgnoreCase(type) && lastIn != null) {
                    totalMinutes += Duration.between(lastIn, time).toMinutes();
                    lastIn = null;
                }
            }

            workDaysMap.put(empId, (long) days.size());
            workMinutesMap.put(empId, totalMinutes);
        }

        // 3) 급여 화면에 내려줄 직원별 DTO 만들기 + 급여 계산 로직
        List<EmployeePayroll> employees = assignments.stream()
            .map(assign -> {
                Long empId = assign.getEmployee().getEmployeeId();

                long workDays = workDaysMap.getOrDefault(empId, 0L);
                long workMinutes = workMinutesMap.getOrDefault(empId, 0L);

                // ✅ [핵심 수정] 계산용(raw)과 표시용(정수)을 분리
                double workHoursRaw = workMinutes / 60.0;      // 🔥 급여 계산용 (기존 로직 그대로)
                double workHours = Math.round(workHoursRaw);   // ✅ UI 표시용 (정수 시간)

                // ✅ 급여설정 가져오기
                PayrollSetting setting = settingMap.get(empId);

                // 3-1) 기본급(설정값) + 급여형태
                long baseWageValue = 0L;           // 설정된 시급/월급
                String wageType = "HOURLY";        // 기본값: 시급제

                if (setting != null) {
                    if (setting.getBaseWage() != null) {
                        baseWageValue = setting.getBaseWage().longValue();
                    }
                    if (setting.getWageType() != null) {
                        wageType = setting.getWageType();
                    }
                }

                // 3-2) 총 지급액(Gross Pay) 계산
                long grossPay;
                if ("MONTHLY".equalsIgnoreCase(wageType)) {
                    grossPay = baseWageValue;
                } else {
                    // ✅ [중요] 급여 계산은 반드시 raw(기존값)로! (정수 workHours 쓰면 꼬임)
                    grossPay = Math.round(baseWageValue * workHoursRaw);
                }

                // 3-3) 공제 정보(JSON) 추출
                DeductionInfo di = extractDeductionInfo(setting);

                // 3-4) 공제액 / 실수령액 계산
                long deductions = Math.round(grossPay * di.getRate());
                long netPay = grossPay - deductions;

                // 3-5) EmployeePayroll DTO 생성
                return new EmployeePayroll(
                    empId,
                    assign.getEmployee().getName(),
                    assign.getRole(),
                    workDays,
                    workHours,          // ✅ UI에는 정수시간으로 내려감
                    baseWageValue,
                    baseWageValue,
                    0L,
                    deductions,
                    netPay,
                    "예정",
                    di.getType(),
                    wageType
                );
            })
            .toList();

        // 4) 급여 이력은 아직 테이블 없으니 빈 리스트로 내려줌
        return new OwnerPayrollResponse(employees, List.of());
    }

    // 🔥 급여 자동 계산 다이얼로그에서 사용하는 합계용 메서드
    @Transactional(readOnly = true)
    public PayrollCalcResultDto calculateMonthlyPayroll(Long storeId, YearMonth yearMonth) {
        // 직원별 계산은 위 메서드를 그대로 재사용
        OwnerPayrollResponse response = getMonthlyPayroll(storeId, yearMonth);

        long totalWorkMinutes = 0L;
        long totalGrossPay = 0L;
        long totalDeductions = 0L;
        long totalNetPay = 0L;

        for (EmployeePayroll e : response.getEmployees()) {
            // workHours → 분 단위로 환산
            long minutes = Math.round(e.getWorkHours() * 60.0);
            totalWorkMinutes += minutes;

            long grossPay = e.getNetPay() + e.getDeductions(); // 총 지급액 = 실수령 + 공제
            totalGrossPay += grossPay;
            totalDeductions += e.getDeductions();
            totalNetPay += e.getNetPay();
        }

        return PayrollCalcResultDto.builder()
                .totalWorkMinutes(totalWorkMinutes)
                .totalGrossPay(totalGrossPay)
                .totalDeductions(totalDeductions)
                .totalNetPay(totalNetPay)
                .employees(response.getEmployees())
                .build();
    }

        /**
     * ✅ deductionItems JSON 에서 공제 타입 / 공제율(rate)을 뽑는 헬퍼
     *  - JSON 예시:
     *      { "type": "FOUR_INSURANCE", "rate": 0.09 }
     *      { "deductionType": "TAX_3_3", "rate": 0.033 }
     */
    private DeductionInfo extractDeductionInfo(PayrollSetting setting) {
        if (setting == null || setting.getDeductionItems() == null) {
            return new DeductionInfo("NONE", 0.0);
        }

        try {
            String json = setting.getDeductionItems();
            if (json == null || json.isBlank()) {
                return new DeductionInfo("NONE", 0.0);
            }

            JsonNode root = objectMapper.readTree(json);

            // 1) type, deductionType 둘 다 지원
            String type = null;
            if (root.hasNonNull("type")) {
                type = root.get("type").asText();
            } else if (root.hasNonNull("deductionType")) {
                type = root.get("deductionType").asText();
            }

            if (type == null || type.isBlank()) {
                type = "NONE";
            }

            // 2) rate 읽기
            double rate = 0.0;
            if (root.has("rate")) {
                rate = root.get("rate").asDouble();
            }

            // 3) rate 가 없으면 type 기준으로 기본값 보정
            if (rate <= 0.0) {
                rate = switch (type) {
                    case "FOUR_INSURANCE" -> 0.09;   // 4대 보험
                    case "TAX_3_3"        -> 0.033;  // 3.3% 공제
                    default               -> 0.0;
                };
            }

            // 4) type 은 없고 rate 만 있을 수도 있으니, rate 로 유추도 해 둠
            if ("NONE".equals(type) && rate > 0.0) {
                if (Math.abs(rate - 0.09) < 0.0001) {
                    type = "FOUR_INSURANCE";
                } else if (Math.abs(rate - 0.033) < 0.0001) {
                    type = "TAX_3_3";
                }
            }

            return new DeductionInfo(type, rate);
        } catch (Exception e) {
            return new DeductionInfo("NONE", 0.0);
        }
    }

    /**
     * ✅ 내부용 공제 정보 DTO
     */
    private static class DeductionInfo {
        private final String type;
        private final double rate;

        public DeductionInfo(String type, double rate) {
            this.type = type;
            this.rate = rate;
        }

        public String getType() {
            return type;
        }

        public double getRate() {
            return rate;
        }
    }
}