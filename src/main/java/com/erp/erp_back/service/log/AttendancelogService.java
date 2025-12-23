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

import com.erp.erp_back.dto.hr.AttendanceShiftStatusResponse;
import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.dto.log.EmployeeAttendanceSummary;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.hr.EmployeeShift;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.AttendanceLogMapper;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.hr.EmployeeShiftRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.StoreGpsRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;
import com.erp.erp_back.service.store.StoreService;

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
    private final StoreService storeService;
    private final StoreGpsRepository storeGpsRepository;

    // ✅ shift 자동 매칭을 위해 사용
    private final EmployeeShiftRepository employeeShiftRepository;

    private static final double EARTH_RADIUS_M = 6371000.0;

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /**
     * 출퇴근 기록 저장 (A안: 서버가 근무시간표 기준으로 shift 자동 매칭)
     */
    public AttendanceLogResponse punch(AttendanceLogRequest req) {
        if (req.getEmployeeId() == null || req.getStoreId() == null) {
            throw new IllegalArgumentException("employeeId, storeId 는 필수입니다.");
        }

        String rawType = (req.getRecordType() == null) ? "" : req.getRecordType().trim().toUpperCase();
        String type = switch (rawType) {
            case "IN", "CLOCK_IN" -> "IN";
            case "OUT", "CLOCK_OUT" -> "OUT";
            default -> "";
        };
        if (!"IN".equals(type) && !"OUT".equals(type)) {
            throw new IllegalArgumentException("recordType 은 IN 또는 OUT 이어야 합니다.");
        }

        Employee emp = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));

        Store store = storeService.requireActiveStore(req.getStoreId());

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

        boolean approved = assignmentRepo.existsApprovedByEmployeeAndStore(emp.getEmployeeId(), store.getStoreId());
        if (!approved) {
            throw new IllegalStateException("해당 매장에 승인된 직원만 출퇴근을 기록할 수 있습니다.");
        }

        // GPS 반경 검사
        if (req.getLatitude() == null || req.getLongitude() == null) {
            throw new IllegalArgumentException("위치 정보가 없습니다. '위치 가져오기' 후 다시 시도해 주세요.");
        }

        StoreGps storeGps = storeGpsRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new IllegalStateException("이 매장은 위치 정보가 설정되어 있지 않습니다. 관리자에게 문의해 주세요."));

        if (storeGps.getLatitude() == null || storeGps.getLongitude() == null) {
            throw new IllegalStateException("이 매장의 위도/경도 정보가 올바르지 않습니다. 관리자에게 문의해 주세요.");
        }

        double radius = (storeGps.getGpsRadiusM() != null) ? storeGps.getGpsRadiusM() : 80.0;
        double distance = distanceMeters(req.getLatitude(), req.getLongitude(), storeGps.getLatitude(), storeGps.getLongitude());

        if (distance > radius) {
            String msg = String.format(
                    "사업장 반경 %.0fm 이내에서만 출퇴근을 기록할 수 있습니다. (현재 거리: 약 %.0fm)",
                    radius,
                    distance
            );
            throw new IllegalStateException(msg);
        }

        // 저장 시각
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = now.toLocalDate();

        // ✅ shift 자동 매칭
        EmployeeShift shift = null;

        if ("IN".equals(type)) {
            LocalTime nowTime = now.toLocalTime();

            // 오늘(shiftDate) + 직원 + 매장 + (startTime <= nowTime <= endTime) 인 shift 찾기
            // (Repository 메서드가 없을 수 있으니, 기본 findAll 후 필터 방식으로 안전하게 처리)
            List<EmployeeShift> todays = employeeShiftRepository
                    .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDate(
                            store.getStoreId(),
                            emp.getEmployeeId(),
                            today
                    );

            // 현재시간을 포함하는 shift를 선택(가장 startTime이 늦은 것 우선)
            shift = todays.stream()
                    .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                    .filter(s -> !nowTime.isBefore(s.getStartTime()) && !nowTime.isAfter(s.getEndTime()))
                    .sorted(Comparator.comparing(EmployeeShift::getStartTime).reversed())
                    .findFirst()
                    .orElse(null);

            if (shift == null) {
                // 근무시간표 연동이 목표이므로, shift를 못 찾으면 출근 자체를 막는 게 안전
                throw new IllegalArgumentException("현재 시간에 해당하는 근무시간표(shift)를 찾을 수 없습니다.");
            }
        } else {
            // OUT: 오늘 최신 IN 로그의 shift 복사
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            List<AttendanceLog> todayLogs = attendanceRepo
                    .findByEmployee_EmployeeIdAndStore_StoreIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                            emp.getEmployeeId(),
                            store.getStoreId(),
                            startOfDay,
                            endOfDay
                    );

            AttendanceLog latestIn = todayLogs.stream()
                    .filter(l -> "IN".equalsIgnoreCase(l.getRecordType()))
                    .findFirst() // 최신순이므로 첫 번째가 최신
                    .orElse(null);

            if (latestIn == null) {
                throw new IllegalStateException("오늘 출근 기록이 없습니다. 먼저 출근을 등록하세요.");
            }

            shift = latestIn.getShift();
            if (shift == null) {
                throw new IllegalStateException("출근 기록에 근무시간표(shift) 연동 정보가 없습니다. 관리자에게 문의해 주세요.");
            }
        }

        // ✅ 중복 방지: "shift 단위"로 IN/OUT 1회씩만 허용
        List<AttendanceLog> shiftLogs = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdOrderByRecordTimeDesc(
                        emp.getEmployeeId(),
                        store.getStoreId(),
                        shift.getShiftId()
                );

        boolean hasInThisShift = shiftLogs.stream().anyMatch(l -> "IN".equalsIgnoreCase(l.getRecordType()));
        boolean hasOutThisShift = shiftLogs.stream().anyMatch(l -> "OUT".equalsIgnoreCase(l.getRecordType()));

        if ("IN".equals(type)) {
            if (hasInThisShift) {
                throw new IllegalStateException("이미 이 근무(shift)에 출근이 등록되어 있습니다.");
            }
        } else { // OUT
            if (!hasInThisShift) {
                throw new IllegalStateException("이 근무(shift)의 출근 기록이 없습니다. 먼저 출근을 등록하세요.");
            }
            if (hasOutThisShift) {
                throw new IllegalStateException("이미 이 근무(shift)에 퇴근이 등록되어 있습니다.");
            }
        }

        AttendanceLog saved = new AttendanceLog();
        saved.setEmployee(emp);
        saved.setStore(store);
        saved.setShift(shift);          // ✅ shift 연동
        saved.setRecordTime(now);
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

        List<AttendanceLog> logs =
                attendanceRepo.findByStoreAndDateTimeRange(storeId, from, to);

        Map<Long, List<AttendanceLog>> logsByEmp = new HashMap<>();
        for (AttendanceLog log : logs) {
            if (log.getEmployee() == null) continue;
            Long empId = log.getEmployee().getEmployeeId();
            logsByEmp.computeIfAbsent(empId, k -> new ArrayList<>()).add(log);
        }

        Map<Long, Integer> daysMap = new HashMap<>();
        Map<Long, Long> minutesMap = new HashMap<>();

        for (Map.Entry<Long, List<AttendanceLog>> entry : logsByEmp.entrySet()) {
            Long empId = entry.getKey();
            List<AttendanceLog> empLogs = entry.getValue();

            empLogs.sort(Comparator.comparing(AttendanceLog::getRecordTime));

            Set<LocalDate> days = new HashSet<>();
            Long totalMinutes = 0L;
            LocalDateTime lastIn = null;

            for (AttendanceLog l : empLogs) {
                LocalDateTime time = l.getRecordTime();
                if (time == null) continue;

                days.add(time.toLocalDate());

                String t = l.getRecordType();
                if ("IN".equalsIgnoreCase(t)) {
                    lastIn = time;
                } else if ("OUT".equalsIgnoreCase(t) && lastIn != null) {
                    totalMinutes += Duration.between(lastIn, time).toMinutes();
                    lastIn = null;
                }
            }

            daysMap.put(empId, days.size());
            minutesMap.put(empId, totalMinutes);
        }

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

    @Transactional(readOnly = true)
    public List<EmployeeAttendanceSummary> findMonthlySummary(
            Long storeId,
            String month,
            Long employeeId
    ) {
        storeService.requireActiveStore(storeId);

        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("month 파라미터는 'yyyy-MM' 형식이어야 합니다.");
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
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

    // =========================
    // ✅ [추가] 상태 API (shift status)
    // =========================
    @Transactional(readOnly = true)
    public AttendanceShiftStatusResponse getShiftStatus(Long employeeId, Long storeId) {

        LocalDateTime nowDt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = nowDt.toLocalDate();
        LocalTime nowTime = nowDt.toLocalTime();

        // 기본 검증
        if (employeeId == null || storeId == null) {
            throw new IllegalArgumentException("employeeId, storeId 는 필수입니다.");
        }

        // 사업장 활성 검증(기존 정책 유지)
        storeService.requireActiveStore(storeId);

        // ✅ 오늘 shifts 조회 (너가 punch()에서 이미 쓰는 repo 메서드 그대로 활용)
        List<EmployeeShift> todays = employeeShiftRepository
                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDate(
                        storeId,
                        employeeId,
                        today
                );

        if (todays == null || todays.isEmpty()) {
            return AttendanceShiftStatusResponse.builder()
                    .storeId(storeId)
                    .employeeId(employeeId)
                    .date(today)
                    .now(nowTime)
                    .shiftId(null)
                    .hasIn(false)
                    .hasOut(false)
                    .canClockIn(false)
                    .canClockOut(false)
                    .message("오늘 등록된 근무시간표(shift)가 없습니다.")
                    .build();
        }

        // ✅ “현재 진행중 shift” 우선 선택
        EmployeeShift current = todays.stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .filter(s -> !nowTime.isBefore(s.getStartTime()) && !nowTime.isAfter(s.getEndTime()))
                .sorted(Comparator.comparing(EmployeeShift::getStartTime).reversed())
                .findFirst()
                .orElse(null);

        // ✅ 현재 shift가 없으면 “다음 shift” 선택 (하루 2번 근무 대응)
        EmployeeShift next = null;
        if (current == null) {
            next = todays.stream()
                    .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                    .filter(s -> nowTime.isBefore(s.getStartTime()))
                    .sorted(Comparator.comparing(EmployeeShift::getStartTime))
                    .findFirst()
                    .orElse(null);
        }

        EmployeeShift target = (current != null) ? current : next;

        // target이 없으면(오늘 shift는 있는데 이미 모두 끝남)
        if (target == null) {
            return AttendanceShiftStatusResponse.builder()
                    .storeId(storeId)
                    .employeeId(employeeId)
                    .date(today)
                    .now(nowTime)
                    .shiftId(null)
                    .hasIn(false)
                    .hasOut(false)
                    .canClockIn(false)
                    .canClockOut(false)
                    .message("오늘 근무시간이 모두 종료되었습니다.")
                    .build();
        }

        Long shiftId = target.getShiftId();

        // ✅ target shift 로그만 조회해서 상태 계산
        List<AttendanceLog> shiftLogs = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdOrderByRecordTimeDesc(
                        employeeId, storeId, shiftId
                );

        boolean hasIn = shiftLogs.stream().anyMatch(l -> "IN".equalsIgnoreCase(l.getRecordType()));
        boolean hasOut = shiftLogs.stream().anyMatch(l -> "OUT".equalsIgnoreCase(l.getRecordType()));

        boolean within = current != null; // current면 근무시간 “진행중”, next면 아직 시작 전

        boolean canIn = within && !hasIn;
        boolean canOut = within && hasIn && !hasOut;

        String msg;
        if (!within) {
            msg = "현재 근무 시간이 아닙니다. 다음 근무 시작 시간에 출근할 수 있습니다.";
        } else if (canIn) {
            msg = "출근 가능합니다.";
        } else if (canOut) {
            msg = "퇴근 가능합니다.";
        } else if (hasIn && hasOut) {
            msg = "이미 이 근무는 출근/퇴근이 완료되었습니다.";
        } else if (hasIn) {
            msg = "이미 출근이 등록되어 있습니다.";
        } else {
            msg = "근무 상태를 확인하세요.";
        }

        return AttendanceShiftStatusResponse.builder()
                .storeId(storeId)
                .employeeId(employeeId)
                .date(today)
                .now(nowTime)
                .shiftId(shiftId)
                .shiftStart(target.getStartTime())
                .shiftEnd(target.getEndTime())
                .hasIn(hasIn)
                .hasOut(hasOut)
                .canClockIn(canIn)
                .canClockOut(canOut)
                .message(msg)
                .build();
    }
}