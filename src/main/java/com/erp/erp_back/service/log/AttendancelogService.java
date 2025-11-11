package com.erp.erp_back.service.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
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

    /**
     * 출퇴근 기록 저장
     * - IN / OUT 체크
     * - 직원-매장 승인 여부 체크
     * - 직전 로그와 순서 체크
     * - ✅ QR 토큰 검사 (attendance_qr_token 기준)
     * - IP로 "허용/차단"은 이제 안 함. 다만 찍어두는 용도로만 저장.
     */
    public AttendanceLogResponse punch(AttendanceLogRequest req, String clientIp) {
        if (req.getEmployeeId() == null || req.getStoreId() == null) {
            throw new IllegalArgumentException("employeeId, storeId 는 필수입니다.");
        }

        // 그냥 보기 좋게 루프백만 정규화
        if ("::1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
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

        // ✅ 최신 QR 토큰 가져와서 검사
        AttendanceQrToken latestToken = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(store.getStoreId())
                .orElse(null);

        if (latestToken != null) {
            // 만료됐으면 실패
            if (latestToken.getExpireAt() != null && latestToken.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("QR 이 만료되었습니다. 다시 발급받아주세요.");
            }

            String reqQr = req.getQrCode();
            if (reqQr == null || reqQr.isBlank() || !latestToken.getTokenValue().equals(reqQr)) {
                throw new IllegalArgumentException("QR 이 일치하지 않습니다. 다시 스캔하세요.");
            }
        }
        // QR 자체가 아직 한 번도 발급 안 된 매장은 통과시키는 정책 그대로 둠

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
            // 첫 기록이 OUT 이면 의미 없음
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
        saved.setClientIp(clientIp);    // ← 그냥 남겨두는 용도

        saved = attendanceRepo.save(saved);

        return AttendanceLogResponse.builder()
                .logId(saved.getLogId())
                .employeeId(emp.getEmployeeId())
                .storeId(store.getStoreId())
                .recordTime(saved.getRecordTime())
                .recordType(saved.getRecordType())
                .clientIp(saved.getClientIp())
                .build();
    }

    /** 특정 직원+매장 최근 기록 */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> recent(Long employeeId, Long storeId) {
        // ✅ 여기서 더 이상 JPQL 메서드 안 쓰고, 페이징 메서드로 대체
        PageRequest pr = PageRequest.of(0, 50); // 필요하면 개수 조절
        Page<AttendanceLog> page = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(employeeId, storeId, pr);

        return page.getContent().stream()
                .map(a -> AttendanceLogResponse.builder()
                        .logId(a.getLogId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .storeId(a.getStore().getStoreId())
                        .recordTime(a.getRecordTime())
                        .recordType(a.getRecordType())
                        .clientIp(a.getClientIp())
                        .build())
                .toList();
    }

    /** 특정 직원+매장 일자별 기록 */
    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> byDay(Long employeeId, Long storeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        // ✅ JPQL 대신 이름 기반 between 메서드 사용
        List<AttendanceLog> rows = attendanceRepo
                .findByEmployee_EmployeeIdAndStore_StoreIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                        employeeId, storeId, start, end);

        return rows.stream()
                .map(a -> AttendanceLogResponse.builder()
                        .logId(a.getLogId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .storeId(a.getStore().getStoreId())
                        .recordTime(a.getRecordTime())
                        .recordType(a.getRecordType())
                        .clientIp(a.getClientIp())
                        .build())
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
                .map(a -> AttendanceLogResponse.builder()
                        .logId(a.getLogId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .storeId(a.getStore().getStoreId())
                        .recordTime(a.getRecordTime())
                        .recordType(a.getRecordType())
                        .clientIp(a.getClientIp())
                        .build())
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
                .map(a -> AttendanceLogResponse.builder()
                        .logId(a.getLogId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .storeId(a.getStore().getStoreId())
                        .recordTime(a.getRecordTime())
                        .recordType(a.getRecordType())
                        .clientIp(a.getClientIp())
                        .build())
                .toList();
    }
}