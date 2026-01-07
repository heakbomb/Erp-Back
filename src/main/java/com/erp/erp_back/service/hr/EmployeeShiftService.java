package com.erp.erp_back.service.hr;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.hr.EmployeeShiftBulkRequest;
import com.erp.erp_back.dto.hr.EmployeeShiftResponse;
import com.erp.erp_back.dto.hr.EmployeeShiftUpsertRequest;
import com.erp.erp_back.entity.hr.EmployeeShift;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.EmployeeShiftMapper;
import com.erp.erp_back.repository.hr.EmployeeShiftRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeShiftService {

    private final EmployeeShiftRepository shiftRepository;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftMapper shiftMapper;
    private final AttendanceLogRepository attendanceLogRepository;

    private static final LocalTime MIDNIGHT = LocalTime.MIDNIGHT;            // 00:00:00
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);    // 23:59:59

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getMonthlyShiftsByStore(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return shiftRepository.findByStore_StoreIdAndShiftDateBetween(storeId, start, end)
                .stream()
                .map(shiftMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getMonthlyShiftsByEmployee(Long storeId, Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return shiftRepository
                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId, start, end)
                .stream()
                .map(shiftMapper::toResponse)
                .toList();
    }

    private void validateShiftTime(LocalTime start, LocalTime end) {
        if (start == null || end == null) return;
        if (start.equals(end)) {
            throw new IllegalStateException("근무 시작/종료 시간이 같을 수 없습니다.");
        }
        // end < start 는 야간으로 허용(분할 저장)
    }

    private void ensureNotDuplicated(Long employeeId, LocalDate date, LocalTime startTime) {
        boolean exists = shiftRepository.existsByEmployee_EmployeeIdAndShiftDateAndStartTime(employeeId, date, startTime);
        if (exists) {
            throw new IllegalStateException("중복근무 신청입니다");
        }
    }

    /**
     * ✅ 그룹 ID 생성 (DB 변경 최소화 목적)
     * - 가장 단순: 현재 millis 기반 (충돌 가능성 낮지만 100%는 아님)
     * - 더 안전하려면 UUID 문자열 + 컬럼 VARCHAR로 가는 게 베스트
     *
     * 지금은 BIGINT 유지 조건이라, 여기서는 millis+난수 조합 사용
     */
    private Long newGroupId() {
        long now = System.currentTimeMillis();
        long rnd = (long) (Math.random() * 1000L);
        return now * 1000L + rnd;
    }

    /**
     * ✅ 단건 upsert
     * - 생성: 당일/야간 분할 저장 + (야간이면 shiftGroupId 부여)
     * - 수정:
     *   - 기존 단일근무(shiftGroupId null) => 기존처럼 1건 수정
     *   - 야간근무(shiftGroupId != null) => 묶음 수정 API 사용 유도
     */
    public EmployeeShiftResponse upsertShift(EmployeeShiftUpsertRequest req) {
        validateShiftTime(req.getStartTime(), req.getEndTime());

        // ✅ 수정 모드
        if (req.getShiftId() != null) {
            EmployeeShift shift = shiftRepository.findById(req.getShiftId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 근무 스케줄을 찾을 수 없습니다."));

            // ✅ 야간(분할) 근무는 묶음 수정으로만 처리 (정합성 깨짐 방지)
            if (shift.getShiftGroupId() != null) {
                throw new IllegalStateException("야간 근무는 묶음 수정으로 변경해주세요.");
            }

            // ✅ 기존 단일 근무 수정은 그대로
            // 단, 수정으로 인해 start==end 들어오면 validate에서 막힘
            // end<start(야간)로 바꾸는 수정은 허용하면 기존 1건을 2건으로 변환해야 해서 정책 깨짐
            if (req.getEndTime().isBefore(req.getStartTime())) {
                throw new IllegalStateException("야간 근무로 변경은 묶음 등록으로 처리해주세요.");
            }

            shiftMapper.updateFromDto(req, shift);
            EmployeeShift saved = shiftRepository.save(shift);
            return shiftMapper.toResponse(saved);
        }

        // ✅ 생성 모드
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));
        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

        LocalDate date = req.getShiftDate();
        LocalTime start = req.getStartTime();
        LocalTime end = req.getEndTime();

        // ✅ 당일 근무
        if (end.isAfter(start)) {
            ensureNotDuplicated(employee.getEmployeeId(), date, start);
            EmployeeShift shift = shiftMapper.toEntity(req, store, employee);
            shift.setShiftGroupId(null); // 당일은 그룹 없음(기존 정책 유지)
            EmployeeShift saved = shiftRepository.save(shift);
            return shiftMapper.toResponse(saved);
        }

        // ✅ 야간 근무(분할 2건) + groupId 부여
        Long groupId = newGroupId();
        LocalDate nextDate = date.plusDays(1);

        ensureNotDuplicated(employee.getEmployeeId(), date, start);
        ensureNotDuplicated(employee.getEmployeeId(), nextDate, MIDNIGHT);

        // part1
        EmployeeShiftUpsertRequest part1 = new EmployeeShiftUpsertRequest();
        part1.setShiftId(null);
        part1.setStoreId(req.getStoreId());
        part1.setEmployeeId(req.getEmployeeId());
        part1.setShiftDate(date);
        part1.setStartTime(start);
        part1.setEndTime(END_OF_DAY);
        part1.setBreakMinutes(req.getBreakMinutes());
        part1.setIsFixed(req.getIsFixed());

        // part2
        EmployeeShiftUpsertRequest part2 = new EmployeeShiftUpsertRequest();
        part2.setShiftId(null);
        part2.setStoreId(req.getStoreId());
        part2.setEmployeeId(req.getEmployeeId());
        part2.setShiftDate(nextDate);
        part2.setStartTime(MIDNIGHT);
        part2.setEndTime(end);
        part2.setBreakMinutes(req.getBreakMinutes());
        part2.setIsFixed(req.getIsFixed());

        EmployeeShift s1 = shiftMapper.toEntity(part1, store, employee);
        EmployeeShift s2 = shiftMapper.toEntity(part2, store, employee);
        s1.setShiftGroupId(groupId);
        s2.setShiftGroupId(groupId);

        EmployeeShift saved1 = shiftRepository.save(s1);
        shiftRepository.save(s2);

        return shiftMapper.toResponse(saved1);
    }

    /**
     * ✅ Bulk upsert
     * - 기존 정책 유지: 하나라도 중복이면 전체 실패
     * - 야간이면 날짜마다 2건 분할 + 날짜별 groupId 부여(각 날짜의 야간근무는 각각 한 묶음)
     */
    public List<EmployeeShiftResponse> upsertBulk(EmployeeShiftBulkRequest req) {
        validateShiftTime(req.getStartTime(), req.getEndTime());

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));
        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

        LocalTime start = req.getStartTime();
        LocalTime end = req.getEndTime();

        // 1) 사전 중복 검증(전체 실패 정책 유지)
        for (LocalDate date : req.getDates()) {
            if (end.isAfter(start)) {
                ensureNotDuplicated(employee.getEmployeeId(), date, start);
            } else {
                ensureNotDuplicated(employee.getEmployeeId(), date, start);
                ensureNotDuplicated(employee.getEmployeeId(), date.plusDays(1), MIDNIGHT);
            }
        }

        // 2) 저장
        List<EmployeeShiftResponse> result = new ArrayList<>();

        for (LocalDate date : req.getDates()) {
            if (end.isAfter(start)) {
                EmployeeShift shift = shiftMapper.toEntityFromBulk(req, store, employee, date);
                shift.setShiftGroupId(null);
                EmployeeShift saved = shiftRepository.save(shift);
                result.add(shiftMapper.toResponse(saved));
            } else {
                Long groupId = newGroupId();

                EmployeeShift part1 = shiftMapper.toEntityFromBulk(req, store, employee, date);
                part1.setStartTime(start);
                part1.setEndTime(END_OF_DAY);
                part1.setShiftGroupId(groupId);

                EmployeeShift part2 = shiftMapper.toEntityFromBulk(req, store, employee, date.plusDays(1));
                part2.setStartTime(MIDNIGHT);
                part2.setEndTime(end);
                part2.setShiftGroupId(groupId);

                EmployeeShift saved1 = shiftRepository.save(part1);
                shiftRepository.save(part2);

                result.add(shiftMapper.toResponse(saved1));
            }
        }

        return result;
    }

    /**
     * ✅ 묶음 수정(야간 2조각 동시 수정)
     * - groupId로 2개를 찾아서:
     *   - 첫날: start ~ 23:59:59
     *   - 다음날: 00:00:00 ~ end
     * - isFixed/breakMinutes도 동일하게 반영
     */
    public List<EmployeeShiftResponse> updateGroup(Long storeId, Long shiftGroupId, EmployeeShiftUpsertRequest req) {
        validateShiftTime(req.getStartTime(), req.getEndTime());

        // 야간 묶음 수정이 목적이므로 end<start가 아니면 막아도 됨(정책 선택)
        if (!req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalStateException("묶음 수정은 야간 근무에만 사용됩니다.");
        }

        List<EmployeeShift> group = shiftRepository.findByShiftGroupIdAndStore_StoreId(shiftGroupId, storeId);
        if (group == null || group.isEmpty()) {
            throw new IllegalArgumentException("해당 근무 묶음을 찾을 수 없습니다.");
        }

        // 직원/스토어 변경은 금지(정합성)
        // 요청에 employeeId/storeId 들어와도 무시
        // 날짜는 “첫 조각의 shiftDate”를 기준으로 유지 (프론트에서 해당 날짜로 수정 요청하게)
        // group 내 2개를 날짜로 정렬
        group.sort((a,b) -> a.getShiftDate().compareTo(b.getShiftDate()));

        EmployeeShift first = group.get(0);
        EmployeeShift second = group.size() > 1 ? group.get(1) : null;

        if (second == null) {
            throw new IllegalStateException("야간 근무 묶음이 완전하지 않습니다.");
        }

        // ✅ 두 조각 시간 재설정
        first.setStartTime(req.getStartTime());
        first.setEndTime(END_OF_DAY);

        second.setStartTime(MIDNIGHT);
        second.setEndTime(req.getEndTime());

        // 공통 속성 반영
        Integer bm = req.getBreakMinutes() == null ? 0 : req.getBreakMinutes();
        Boolean fixed = req.getIsFixed() != null && req.getIsFixed();

        first.setBreakMinutes(bm);
        second.setBreakMinutes(bm);
        first.setIsFixed(fixed);
        second.setIsFixed(fixed);

        // ✅ 저장
        shiftRepository.save(first);
        shiftRepository.save(second);

        return group.stream().map(shiftMapper::toResponse).toList();
    }

    /**
     * ✅ 묶음 삭제
     * - groupId에 속한 shiftId들을 먼저 뽑아서 attendance_log 참조 끊고 삭제
     */
    public void deleteGroup(Long storeId, Long shiftGroupId) {
        List<EmployeeShift> group = shiftRepository.findByShiftGroupIdAndStore_StoreId(shiftGroupId, storeId);
        if (group == null || group.isEmpty()) return;

        List<Long> shiftIds = group.stream().map(EmployeeShift::getShiftId).toList();
        attendanceLogRepository.detachShiftRefs(shiftIds);

        shiftRepository.deleteByShiftGroupId(shiftGroupId);
    }

    // 기존 단건 삭제 유지
    public void deleteShift(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new IllegalArgumentException("삭제할 근무 스케줄이 존재하지 않습니다.");
        }
        attendanceLogRepository.detachShiftRefs(List.of(shiftId));
        shiftRepository.deleteById(shiftId);
    }

    // 기존 기간 삭제 유지
    @Transactional
    public void deleteRange(Long storeId, Long employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        List<EmployeeShift> targets = shiftRepository
                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId, from, to);

        if (targets.isEmpty()) return;

        List<Long> shiftIds = targets.stream().map(EmployeeShift::getShiftId).toList();
        attendanceLogRepository.detachShiftRefs(shiftIds);
        shiftRepository.deleteAllByIdInBatch(shiftIds);
    }
}