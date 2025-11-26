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

        // ✅ 1) 특정 가게의 월간 근무표 조회 (전체 직원)
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

        // ✅ 2) 특정 가게 + 특정 직원의 월간 근무표
        @Transactional(readOnly = true)
        public List<EmployeeShiftResponse> getMonthlyShiftsByEmployee(
                        Long storeId, Long employeeId, int year, int month) {
                YearMonth ym = YearMonth.of(year, month);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();

                return shiftRepository
                                .findByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId, start, end)
                                .stream()
                                .map(shiftMapper::toResponse)
                                .toList();
        }

        // 3) 근무 스케줄 생성/수정
        public EmployeeShiftResponse upsertShift(EmployeeShiftUpsertRequest req) {
                // 생성 시 중복 체크
                if (req.getShiftId() == null) {
                        boolean exists = shiftRepository.existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                                        req.getEmployeeId(), req.getShiftDate(), req.getStartTime());
                        if (exists) {
                                throw new IllegalStateException("이미 등록된 근무 스케줄입니다.");
                        }
                }

                EmployeeShift shift;
                if (req.getShiftId() != null) {
                        // [수정] 기존 엔티티 조회 -> Mapper로 업데이트
                        shift = shiftRepository.findById(req.getShiftId())
                                        .orElseThrow(() -> new IllegalArgumentException("해당 근무 스케줄을 찾을 수 없습니다."));
                        shiftMapper.updateFromDto(req, shift);
                } else {
                        // [생성] Mapper로 엔티티 생성
                        Store store = storeRepository.findById(req.getStoreId())
                                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));
                        Employee employee = employeeRepository.findById(req.getEmployeeId())
                                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

                        shift = shiftMapper.toEntity(req, store, employee);
                }

                EmployeeShift saved = shiftRepository.save(shift);
                return shiftMapper.toResponse(saved);
        }

        // 4) Bulk Upsert
        public List<EmployeeShiftResponse> upsertBulk(EmployeeShiftBulkRequest req) {
                Store store = storeRepository.findById(req.getStoreId())
                                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업장입니다."));

                Employee employee = employeeRepository.findById(req.getEmployeeId())
                                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직원입니다."));

                List<EmployeeShiftResponse> result = new ArrayList<>();

                for (LocalDate date : req.getDates()) {
                        boolean exists = shiftRepository.existsByEmployee_EmployeeIdAndShiftDateAndStartTime(
                                        employee.getEmployeeId(), date, req.getStartTime());
                        if (exists) {
                                continue;
                        }

                        // ✅ Mapper를 사용하여 Entity 생성 (날짜만 Loop 변수 주입)
                        EmployeeShift shift = shiftMapper.toEntityFromBulk(req, store, employee, date);
                        EmployeeShift saved = shiftRepository.save(shift);
                        result.add(shiftMapper.toResponse(saved));
                }

                return result;
        }

        // 5) 삭제
        public void deleteShift(Long shiftId) {
                if (!shiftRepository.existsById(shiftId)) {
                        throw new IllegalArgumentException("삭제할 근무 스케줄이 존재하지 않습니다.");
                }
                shiftRepository.deleteById(shiftId);
        }

        // 6) 기간 삭제
        @Transactional
        public void deleteRange(Long storeId, Long employeeId, LocalDate from, LocalDate to) {
                if (from.isAfter(to)) {
                        LocalDate tmp = from;
                        from = to;
                        to = tmp;
                }
                shiftRepository.deleteByStore_StoreIdAndEmployee_EmployeeIdAndShiftDateBetween(storeId, employeeId, from, to);
        }
}