package com.erp.erp_back.service.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.AttendanceLogMapper;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendancelogService {

    private final AttendanceLogRepository attendanceRepo;
    private final EmployeeAssignmentRepository assignmentRepo;
    private final EmployeeRepository employeeRepo;
    private final StoreRepository storeRepo;
    private final AttendanceQrTokenRepository attendanceQrTokenRepository;
    private final AttendanceLogMapper attendanceLogMapper;

    /**
     * 출퇴근 기록 저장
     */
    public AttendanceLogResponse punch(AttendanceLogRequest req) {
        if (req.getEmployeeId() == null || req.getStoreId() == null) {
            throw new IllegalArgumentException("employeeId, storeId 는 필수입니다.");
        }

        // recordType 정규화
        String rawType = (req.getRecordType() == null) ? "" : req.getRecordType().trim().toUpperCase();
        String type = switch (rawType) {
            case "IN", "CLOCK_IN" -> "IN";
            case "OUT", "CLOCK_OUT" -> "OUT";
            default -> "";
        };
        if (!"IN".equals(type) && !"OUT".equals(type)) {
            throw new IllegalArgumentException("recordType 은 IN 또는 OUT 이어야 합니다.");
        }

        // 직원 / 매장 존재 확인
        Employee emp = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));
        Store store = storeRepo.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        // 최신 QR 토큰 검사
        AttendanceQrToken latestToken = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(store.getStoreId())
                .orElse(null);

        if (latestToken != null) {
            if (latestToken.getExpireAt() != null && latestToken.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("QR 이 만료되었습니다. 다시 발급받아주세요.");
            }

            String reqQr = req.getQrCode();
            if (reqQr == null || reqQr.isBlank() || !latestToken.getTokenValue().equals(reqQr)) {
                throw new IllegalArgumentException("QR 이 일치하지 않습니다. 다시 스캔하세요.");
            }
        }

        // 직원이 이 매장에 승인 상태인지 확인
        boolean approved = assignmentRepo.existsApprovedByEmployeeAndStore(emp.getEmployeeId(), store.getStoreId());
        if (!approved) {
            throw new IllegalStateException("해당 매장에 승인된 직원만 출퇴근을 기록할 수 있습니다.");
        }

        // 마지막 기록 확인해서 IN/OUT 순서 체크
        AttendanceLog last = attendanceRepo
                .findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(
                        emp.getEmployeeId(), store.getStoreId());

        if (last != null) {
            if ("IN".equals(last.getRecordType()) && "IN".equals(type)) {
                throw new IllegalStateException("이미 출근 상태입니다. 먼저 퇴근(OUT)을 기록하세요.");
            }
            if ("OUT".equals(last.getRecordType()) && "OUT".equals(type)) {
                throw new IllegalStateException("이미 퇴근 상태입니다. 먼저 출근(IN)을 기록하세요.");
            }
        } else {
            if ("OUT".equals(type)) {
                throw new IllegalStateException("첫 기록은 출근(IN)이어야 합니다.");
            }
        }

        // 저장
        AttendanceLog saved = new AttendanceLog();
        saved.setEmployee(emp);
        saved.setStore(store);
        saved.setRecordTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        saved.setRecordType(type);

        saved = attendanceRepo.save(saved);

        return attendanceLogMapper.toResponse(saved);
    }

    /** 특정 직원+매장 최근 기록 */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> recent(Long employeeId, Long storeId) {
        PageRequest pr = PageRequest.of(0, 50);
        Page<AttendanceLog> page = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(employeeId, storeId, pr);

        return page.getContent().stream()
                .map(attendanceLogMapper::toResponse)
                .toList();
    }

    /** 특정 직원+매장 일자별 기록 */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> byDay(Long employeeId, Long storeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<AttendanceLog> rows = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                        employeeId, storeId, start, end);

        return rows.stream()
                .map(attendanceLogMapper::toResponse)
                .toList();
    }

    // =========================
    // 직원 본인 조회용
    // =========================

    /** 직원 본인 최근 N건 (storeId 없으면 전체 매장 기준) */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> myRecent(Long employeeId, Integer limit, Long storeId) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 30 : limit;
        PageRequest pr = PageRequest.of(0, size);

        Page<AttendanceLog> page = (storeId == null)
                ? attendanceRepo.findByEmployee_EmployeeIdOrderByRecordTimeDesc(employeeId, pr)
                : attendanceRepo.findByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(employeeId, storeId, pr);

        return page.getContent().stream()
                .map(attendanceLogMapper::toResponse)
                .toList();
    }

    /** 직원 본인 기간 조회 */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> myRange(Long employeeId, LocalDate from, LocalDate to, Long storeId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<AttendanceLog> rows = (storeId == null)
                ? attendanceRepo.findByEmployee_EmployeeIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                        employeeId, start, end)
                : attendanceRepo.findByEmployee_EmployeeIdAndStore_StoreIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                        employeeId, storeId, start, end);

        return rows.stream()
                .map(attendanceLogMapper::toResponse)
                .toList();
    }

    // =========================
    // 사장페이지용 - 매장 로그 리스트 조회
    // =========================
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> findLogsForOwner(
            Long storeId,
            LocalDate from,
            LocalDate to,
            Long employeeId
    ) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<AttendanceLog> rows =
                attendanceRepo.findByStoreAndDateTimeRange(storeId, start, end);

        if (employeeId != null) {
            rows = rows.stream()
                    .filter(l -> l.getEmployee() != null
                            && employeeId.equals(l.getEmployee().getEmployeeId()))
                    .toList();
        }

        return rows.stream()
                .map(attendanceLogMapper::toResponse)
                .toList();
    }

    // =========================
    // 사장페이지용 - 직원 출결 "월간 요약" (기본 로직)
    // =========================
    @Transactional(readOnly = true)
    public List<EmployeeAttendanceSummary> getOwnerMonthlySummary(
            Long storeId,
            LocalDate anyDateInMonth
    ) {
        LocalDate firstDay = anyDateInMonth.withDayOfMonth(1);
        LocalDate lastDay = anyDateInMonth.withDayOfMonth(anyDateInMonth.lengthOfMonth());

        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = lastDay.atTime(LocalTime.MAX);

        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        // 1) 이 매장의 이 달 전체 로그
        List<AttendanceLog> logs =
                attendanceRepo.findByStoreAndDateTimeRange(storeId, from, to);

        // 2) 직원별로 로그 그룹핑
        Map<Long, List<AttendanceLog>> logsByEmp = new HashMap<>();
        for (AttendanceLog log : logs) {
            if (log.getEmployee() == null) continue;
            Long empId = log.getEmployee().getEmployeeId();
            logsByEmp.computeIfAbsent(empId, k -> new ArrayList<>()).add(log);
        }

        // 3) 직원별 근무일수 / 근무시간 계산
        Map<Long, Integer> daysMap = new HashMap<>();
        Map<Long, Long> minutesMap = new HashMap<>();

        for (Map.Entry<Long, List<AttendanceLog>> entry : logsByEmp.entrySet()) {
            Long empId = entry.getKey();
            List<AttendanceLog> empLogs = entry.getValue();

            // 시간 순 정렬
            empLogs.sort(Comparator.comparing(AttendanceLog::getRecordTime));

            Set<LocalDate> days = new HashSet<>();
            Long totalMinutes = 0L;
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

            daysMap.put(empId, days.size());
            minutesMap.put(empId, totalMinutes);
        }

        // 4) 승인된 직원 기준으로 결과 구성
        List<EmployeeAssignment> assignments = assignmentRepo.findAllByStoreId(storeId);

        List<EmployeeAttendanceSummary> result = new ArrayList<>();

        for (EmployeeAssignment assign : assignments) {
            if (assign.getStatus() == null ||
                    !assign.getStatus().equalsIgnoreCase("APPROVED")) {
                continue;
            }

            Employee emp = assign.getEmployee();
            Long empId = emp.getEmployeeId();

            int days = daysMap.getOrDefault(empId, 0);
            long minutes = minutesMap.getOrDefault(empId, 0L);
            double hours = minutes / 60.0;

            EmployeeAttendanceSummary summary = new EmployeeAttendanceSummary(
                    empId,
                    emp.getName(),
                    storeId,
                    store.getStoreName(),
                    days,
                    hours
            );
            result.add(summary);
        }

        return result;
    }

    // =========================
    // 사장페이지용 - 직원 출결 "월간 요약" + 직원 필터
    // =========================
    @Transactional(readOnly = true)
    public List<EmployeeAttendanceSummary> findMonthlySummary(
            Long storeId,
            String month,
            Long employeeId
    ) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("month 파라미터는 'yyyy-MM' 형식이어야 합니다.");
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(month);   // 예: "2025-11"
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month 파라미터는 'yyyy-MM' 형식이어야 합니다.");
        }

        LocalDate anyDateInMonth = ym.atDay(1);
        List<EmployeeAttendanceSummary> all = getOwnerMonthlySummary(storeId, anyDateInMonth);

        if (employeeId == null) {
            return all;
        }

        List<EmployeeAttendanceSummary> filtered = new ArrayList<>();
        for (EmployeeAttendanceSummary s : all) {
            if (employeeId.equals(s.getEmployeeId())) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}