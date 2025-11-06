package com.erp.erp_back.service.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.StoreQrResponse;
import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreRepository;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessNumberRepository businessNumberRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final AttendanceQrTokenRepository attendanceQrTokenRepository;

    public StoreService(
            StoreRepository storeRepository,
            BusinessNumberRepository businessNumberRepository,
            EmployeeAssignmentRepository assignmentRepository,
            AttendanceQrTokenRepository attendanceQrTokenRepository
    ) {
        this.storeRepository = storeRepository;
        this.businessNumberRepository = businessNumberRepository;
        this.assignmentRepository = assignmentRepository;
        this.attendanceQrTokenRepository = attendanceQrTokenRepository;
    }

    // 사업장 등록
    public StoreResponse createStore(StoreCreateRequest request) {
        BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));

        Store store = Store.builder()
                .storeName(request.getStoreName())
                .industry(request.getIndustry())
                .posVendor(request.getPosVendor())
                .status("PENDING")
                .businessNumber(bn)
                .build();

        // ✅ 여기서 위도/경도도 같이 세팅
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());

        Store saved = storeRepository.save(store);
        return StoreResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        return StoreResponse.from(store);
    }

    public StoreResponse updateStore(Long storeId, StoreCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장 없음"));

        store.setStoreName(request.getStoreName());
        store.setIndustry(request.getIndustry());
        store.setPosVendor(request.getPosVendor());

        // ✅ 수정할 때도 위도/경도 반영
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());

        if (request.getBizId() != null) {
            BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));
            store.setBusinessNumber(bn);
        }

        return StoreResponse.from(store);
    }

    public void deleteStore(Long storeId) {
        deleteStore(storeId, false);
    }

    public void deleteStore(Long storeId, boolean force) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));

        boolean hasChildren = assignmentRepository.existsByStore_StoreId(storeId);

        if (hasChildren && !force) {
            throw new IllegalStateException("해당 사업장에 연결된 근무 신청/배정이 있어 삭제할 수 없습니다. (force=true로 강제 삭제 가능)");
        }

        if (hasChildren) {
            assignmentRepository.deleteByStore_StoreId(storeId);
        }

        storeRepository.deleteById(storeId);
    }

    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByOwner(Long ownerId) {
        List<Store> rows = storeRepository.findAllByOwnerId(ownerId);
        return rows.stream()
                .map(StoreSimpleResponse::from)
                .collect(Collectors.toList());
    }

    // =============================
    // ✅ 여기부터 QR 관련 (attendance_qr_token만 사용)
    // =============================

    @Transactional
    public StoreQrResponse regenerateQrToken(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));

        attendanceQrTokenRepository.deleteByStore_StoreId(storeId);

        String token = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusMinutes(5);

        AttendanceQrToken qr = new AttendanceQrToken();
        qr.setStore(store);
        qr.setTokenValue(token);
        qr.setExpireAt(expires);

        attendanceQrTokenRepository.save(qr);

        return StoreQrResponse.builder()
                .storeId(storeId)
                .qrToken(token)
                .expireAt(expires)
                .build();
    }

    @Transactional(readOnly = true)
    public StoreQrResponse getQrToken(Long storeId) {
        AttendanceQrToken latest = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장의 QR 토큰이 없습니다."));

        return StoreQrResponse.builder()
                .storeId(storeId)
                .qrToken(latest.getTokenValue())
                .expireAt(latest.getExpireAt())
                .build();
    }

    @Transactional
    public StoreQrResponse getOrRefreshQr(Long storeId, boolean refresh) {
        if (refresh) {
            return regenerateQrToken(storeId);
        }

        AttendanceQrToken latest = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(storeId)
                .orElse(null);

        if (latest == null || latest.getExpireAt().isBefore(LocalDateTime.now())) {
            return regenerateQrToken(storeId);
        }

        return StoreQrResponse.builder()
                .storeId(storeId)
                .qrToken(latest.getTokenValue())
                .expireAt(latest.getExpireAt())
                .build();
    }
}