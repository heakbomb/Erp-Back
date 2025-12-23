package com.erp.erp_back.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.hr.EmployeeShift;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.repository.log.AttendanceLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceAutoCheckoutScheduler {

    private final AttendanceLogRepository attendanceLogRepository;

    /**
     * ✅ 자동 퇴근 스케줄러
     * - 1분마다 실행
     * - shift 종료시간 + 1시간 경과 시 자동 OUT
     */
    @Scheduled(fixedDelay = 60_000) // 1분
    @Transactional
    public void autoCheckout() {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // ✅ [임시] 테스트용 storeId (나중에 매장 loop로 확장)
        Long storeId = 11L;

        // OUT 없는 IN 로그 조회 (shift 단위)
        List<AttendanceLog> openInLogs =
                attendanceLogRepository.findOpenInLogsByStoreAndDate(storeId, today);

        for (AttendanceLog inLog : openInLogs) {

            EmployeeShift shift = inLog.getShift();
            if (shift == null) continue;

            // shift 종료 시각
            LocalDateTime shiftEnd =
                    LocalDateTime.of(shift.getShiftDate(), shift.getEndTime());

            // 종료 + 1시간
            LocalDateTime autoOutAt = shiftEnd.plusHours(1);

            // 아직 시간이 안 됐으면 skip
            if (now.isBefore(autoOutAt)) continue;

            Long employeeId = inLog.getEmployee().getEmployeeId();
            Long shiftId = shift.getShiftId();

            // 중복 방지 (shift 기준)
            boolean alreadyOut =
                    attendanceLogRepository
                            .existsByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdAndRecordType(
                                    employeeId,
                                    storeId,
                                    shiftId,
                                    "OUT"
                            );

            if (alreadyOut) continue;

            // 자동 OUT 생성
            AttendanceLog autoOut = new AttendanceLog();
            autoOut.setEmployee(inLog.getEmployee());
            autoOut.setStore(inLog.getStore());
            autoOut.setShift(shift);
            autoOut.setRecordType("OUT");
            autoOut.setRecordTime(autoOutAt); // 요구사항 그대로

            attendanceLogRepository.save(autoOut);

            log.info(
                "[AUTO-CHECKOUT] employeeId={}, storeId={}, shiftId={}, time={}",
                employeeId,
                storeId,
                shiftId,
                autoOutAt
            );
        }
    }
}