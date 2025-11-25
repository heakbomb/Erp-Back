package com.erp.erp_back.service.hr;

import java.time.LocalDate;
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
import com.erp.erp_back.repository.hr.EmployeeShiftRepository;
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

    // ✅ 1) 특정 가게의 월간 근무표 조회 (전체 직원)
    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getMonthlyShiftsByStore(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<EmployeeShift> rows = shiftRepository
                .findByStore_StoreIdAndShiftDateBetween(storeId, start, end);

        return rows.stream()
                .map(EmployeeShiftResponse::from)
                .toList();
    }

    // ✅ 2) 특정 가게 + 특정 직원의 월간 근무표
    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getMonthlyShiftsByEmployee(
            Long storeId, Long employeeId, int year, int month
    ) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<EmployeeShift> rows = shiftRepository
                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(
                        storeId, employeeId, start, end
                );

        return rows.stream()
                .map(EmployeeShiftResponse::from)
                .toList();
    }

    // ✅ 3) 근무 스케줄 생성/수정 (단건 Upsert)
    public EmployeeShiftResponse upsertShift(EmployeeShiftUpsertRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));

        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

        // ✅ 새로 생성할 때만 중복 체크
        if (req.getShiftId() == null) {
            boolean exists = shiftRepository
                    .existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                            req.getEmployeeId(),
                            req.getShiftDate(),
                            req.getStartTime()
                    );
            if (exists) {
                // 프론트에서 alert/토스트로 잡아서
                // "이미 등록된 근무 스케줄입니다." 같은 메시지 노출
                throw new IllegalStateException("이미 등록된 근무 스케줄입니다.");
            }
        }

        EmployeeShift shift;

        if (req.getShiftId() != null) {
            // 수정
            shift = shiftRepository.findById(req.getShiftId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 근무 스케줄을 찾을 수 없습니다."));
        } else {
            // 생성
            shift = new EmployeeShift();
        }

        shift.setStore(store);
        shift.setEmployee(employee);
        shift.setShiftDate(req.getShiftDate());
        shift.setStartTime(req.getStartTime());
        shift.setEndTime(req.getEndTime());
        shift.setIsFixed(Boolean.TRUE.equals(req.getIsFixed()));

        // ✅ 휴게 시간 반영 (null이면 0)
        Integer breakMinutes = req.getBreakMinutes();
        shift.setBreakMinutes(breakMinutes == null ? 0 : breakMinutes);

        EmployeeShift saved = shiftRepository.save(shift);
        return EmployeeShiftResponse.from(saved);
    }

    // ✅ 4) 여러 날짜를 한 번에 생성/수정 (Bulk Upsert)
    public List<EmployeeShiftResponse> upsertBulk(EmployeeShiftBulkRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));

        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

        int breakMinutes = req.getBreakMinutes() == null ? 0 : req.getBreakMinutes();
        boolean isFixed = req.getIsFixed() != null ? req.getIsFixed() : false;

        List<EmployeeShiftResponse> result = new ArrayList<>();

        for (LocalDate date : req.getDates()) {

            // ✅ 동일 직원 + 날짜 + 시작시간이 이미 있으면 건너뜀
            boolean exists = shiftRepository
                    .existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                            employee.getEmployeeId(),
                            date,
                            req.getStartTime()
                    );
            if (exists) {
                // 이미 등록된 근무는 스킵(예외 던지지 않고 다음 날짜 계속)
                continue;
            }

            EmployeeShift shift = new EmployeeShift();
            shift.setStore(store);
            shift.setEmployee(employee);
            shift.setShiftDate(date);
            shift.setStartTime(req.getStartTime());
            shift.setEndTime(req.getEndTime());
            shift.setBreakMinutes(breakMinutes);
            shift.setIsFixed(isFixed);

            EmployeeShift saved = shiftRepository.save(shift);
            result.add(EmployeeShiftResponse.from(saved));
        }

        return result;
    }

    // ✅ 5) 근무 스케줄 삭제
    public void deleteShift(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new IllegalArgumentException("삭제할 근무 스케줄이 존재하지 않습니다.");
        }
        shiftRepository.deleteById(shiftId);
    }

     // ✅ 6) 근무 스케줄 일괄 삭제 (기간)
    @Transactional
    public void deleteRange(Long storeId, Long employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            // from, to가 뒤바뀐 경우 자동 보정해도 됨 (선택)
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        shiftRepository.deleteByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(
                storeId,
                employeeId,
                from,
                to
        );
    }
}