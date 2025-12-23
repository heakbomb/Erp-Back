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
import com.erp.erp_back.mapper.EmployeeShiftMapper;
import com.erp.erp_back.repository.hr.EmployeeShiftRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository; // ✅ 추가
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

        // ✅ 삭제 안전장치용(shift 참조 끊기)
        private final AttendanceLogRepository attendanceLogRepository; // ✅ 추가

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
        public List<EmployeeShiftResponse> getMonthlyShiftsByEmployee(
                        Long storeId, Long employeeId, int year, int month) {
                YearMonth ym = YearMonth.of(year, month);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();

                return shiftRepository
                                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId,
                                                start, end)
                                .stream()
                                .map(shiftMapper::toResponse)
                                .toList();
        }

        public EmployeeShiftResponse upsertShift(EmployeeShiftUpsertRequest req) {
                if (req.getShiftId() == null) {
                        boolean exists = shiftRepository.existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                                        req.getEmployeeId(), req.getShiftDate(), req.getStartTime());
                        if (exists) {
                                throw new IllegalStateException("이미 등록된 근무 스케줄입니다.");
                        }
                }

                EmployeeShift shift;
                if (req.getShiftId() != null) {
                        shift = shiftRepository.findById(req.getShiftId())
                                        .orElseThrow(() -> new IllegalArgumentException("해당 근무 스케줄을 찾을 수 없습니다."));
                        shiftMapper.updateFromDto(req, shift);
                } else {
                        Store store = storeRepository.findById(req.getStoreId())
                                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));
                        Employee employee = employeeRepository.findById(req.getEmployeeId())
                                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

                        shift = shiftMapper.toEntity(req, store, employee);
                }

                EmployeeShift saved = shiftRepository.save(shift);
                return shiftMapper.toResponse(saved);
        }

        public List<EmployeeShiftResponse> upsertBulk(EmployeeShiftBulkRequest req) {
                Store store = storeRepository.findById(req.getStoreId())
                                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));

                Employee employee = employeeRepository.findById(req.getEmployeeId())
                                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

                for (LocalDate date : req.getDates()) {
                        boolean exists = shiftRepository.existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                                        employee.getEmployeeId(), date, req.getStartTime());
                        if (exists) {
                                throw new IllegalStateException("이미 등록된 근무 스케줄이 포함되어 있습니다.");
                        }
                }

                List<EmployeeShiftResponse> result = new ArrayList<>();

                for (LocalDate date : req.getDates()) {
                        EmployeeShift shift = shiftMapper.toEntityFromBulk(req, store, employee, date);
                        EmployeeShift saved = shiftRepository.save(shift);
                        result.add(shiftMapper.toResponse(saved));
                }

                return result;
        }

        // ✅ 5) 삭제 (핵심: 삭제 전에 attendance_log.shift_id를 null로 끊어서 FK 이슈 방지)
        public void deleteShift(Long shiftId) {
                if (!shiftRepository.existsById(shiftId)) {
                        throw new IllegalArgumentException("삭제할 근무 스케줄이 존재하지 않습니다.");
                }

                // FK가 DB에 SET NULL로 걸려 있어도, 팀 환경(마이그레이션 차이) 대비해서 선제적으로 끊어줌
                attendanceLogRepository.detachShiftRefs(List.of(shiftId));

                shiftRepository.deleteById(shiftId);
        }

        // ✅ 6) 기간 삭제 (핵심: 대상 shiftId들을 먼저 조회 → 로그 참조 끊기 → shift 삭제)
        @Transactional
        public void deleteRange(Long storeId, Long employeeId, LocalDate from, LocalDate to) {
                if (from.isAfter(to)) {
                        LocalDate tmp = from;
                        from = to;
                        to = tmp;
                }

                // 1) 삭제 대상 shift를 조회해서 shiftId 목록 확보
                List<EmployeeShift> targets = shiftRepository
                                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId, from, to);

                if (targets.isEmpty()) return;

                List<Long> shiftIds = targets.stream().map(EmployeeShift::getShiftId).toList();

                // 2) attendance_log의 FK 참조를 먼저 끊기
                attendanceLogRepository.detachShiftRefs(shiftIds);

                // 3) shift 삭제
                shiftRepository.deleteAllByIdInBatch(shiftIds);
        }
}