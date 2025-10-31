package com.erp.erp_back.service.log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.AttendanceLogRequest;
import com.erp.erp_back.dto.log.AttendanceLogResponse;
import com.erp.erp_back.entity.log.AttendanceLog;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceLogRepository;
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

    /**
     * 출퇴근 기록 저장
     * 규칙:
     * - recordType: IN/OUT 만 허용 (✅ CLOCK_IN/CLOCK_OUT도 자동 매핑)
     * - 직원-사업장 승인(APPROVED) 확인
     * - 직전 로그와의 연속성(IN 다음은 OUT, OUT 다음은 IN)
     * - ✅ 회사 네트워크( store.allowedCidrList ) 허용 범위 내에서만 기록
     */
    public AttendanceLogResponse punch(AttendanceLogRequest req, String clientIp) {
        if (req.getEmployeeId() == null || req.getStoreId() == null) {
            throw new IllegalArgumentException("employeeId, storeId 는 필수입니다.");
        }

        // ✅ IPv6 루프백을 IPv4로 정규화
        if ("::1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }

        // ✅ recordType 정규화 (CLOCK_IN/CLOCK_OUT -> IN/OUT)
        String rawType = (req.getRecordType() == null) ? "" : req.getRecordType().trim().toUpperCase();
        String type = switch (rawType) {
            case "IN", "CLOCK_IN" -> "IN";
            case "OUT", "CLOCK_OUT" -> "OUT";
            default -> "";
        };
        if (!"IN".equals(type) && !"OUT".equals(type)) {
            throw new IllegalArgumentException("recordType 은 IN 또는 OUT 이어야 합니다.");
        }

        // 직원/매장 존재 확인
        Employee emp = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직원입니다."));
        Store store = storeRepo.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사업장입니다."));

        // ✅ 회사 네트워크(IP/CIDR) 허용 여부 체크
        if (!isIpAllowed(clientIp, store.getAllowedCidrList())) {
            throw new IllegalArgumentException("허용되지 않은 네트워크에서의 출퇴근입니다. (IP: " + clientIp + ")");
        }

        // 승인 여부 확인
        boolean approved = assignmentRepo.existsApprovedByEmployeeAndStore(emp.getEmployeeId(), store.getStoreId());
        if (!approved) {
            throw new IllegalStateException("해당 매장에 승인된 직원만 출퇴근을 기록할 수 있습니다.");
        }

        // 마지막 기록 확인해서 IN/OUT 연속성 체크
        AttendanceLog last = attendanceRepo
                .findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(emp.getEmployeeId(), store.getStoreId());

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

        AttendanceLog saved = new AttendanceLog();
        saved.setEmployee(emp);
        saved.setStore(store);
        // 요청값 있으면 사용, 없으면 now
        saved.setRecordTime(req.getRecordTime() != null ? req.getRecordTime() : LocalDateTime.now());
        saved.setRecordType(type);
        saved.setClientIp(clientIp);

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

    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> recent(Long employeeId, Long storeId) {
        List<AttendanceLog> rows = attendanceRepo.findLatestLogs(employeeId, storeId);
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

    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> byDay(Long employeeId, Long storeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<AttendanceLog> rows = attendanceRepo.findDayLogs(employeeId, storeId, start, end);
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

    // -----------------------
    // 내부 유틸: IP 허용 로직
    // -----------------------
    private boolean isIpAllowed(String clientIp, String allowedListCsv) {
        if (clientIp == null || clientIp.isBlank()) return false;

        // ✅ 로컬 개발 편의 허용(IPv4/IPv6 루프백)
        if ("127.0.0.1".equals(clientIp) || "::1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            return true;
        }

        if (allowedListCsv == null || allowedListCsv.isBlank()) return false;

        String[] tokens = allowedListCsv.split(",");
        for (String raw : tokens) {
            String token = raw.trim();
            if (token.isEmpty()) continue;

            // 단일 IP
            if (!token.contains("/")) {
                if (clientIp.equals(token)) return true;
                continue;
            }

            // IPv4 CIDR
            if (cidrContains(token, clientIp)) return true;
        }
        return false;
    }

    /** 간단 IPv4 CIDR 포함 검사(개발용) */
    @SuppressWarnings("UseSpecificCatch")
    private boolean cidrContains(String cidr, String ip) {
        try {
            String[] parts = cidr.split("/");
            String cidrIp = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            int ipInt = ipv4ToInt(ip);
            int cidrInt = ipv4ToInt(cidrIp);
            int mask = prefix == 0 ? 0 : (int)(0xFFFFFFFFL << (32 - prefix));

            return (ipInt & mask) == (cidrInt & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private int ipv4ToInt(String ipv4) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(ipv4).getAddress();
        return ((bytes[0] & 0xFF) << 24)
             | ((bytes[1] & 0xFF) << 16)
             | ((bytes[2] & 0xFF) << 8)
             |  (bytes[3] & 0xFF);
    }
}