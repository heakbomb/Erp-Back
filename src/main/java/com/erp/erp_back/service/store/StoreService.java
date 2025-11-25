package com.erp.erp_back.service.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.StoreQrResponse;
import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreGpsRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.dto.store.BusinessNumberResponse; 

import jakarta.persistence.criteria.Predicate;
@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessNumberRepository businessNumberRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final AttendanceQrTokenRepository attendanceQrTokenRepository;
    private final StoreGpsRepository storeGpsRepository;   

    public StoreService(
            StoreRepository storeRepository,
            BusinessNumberRepository businessNumberRepository,
            EmployeeAssignmentRepository assignmentRepository,
            AttendanceQrTokenRepository attendanceQrTokenRepository,
            StoreGpsRepository storeGpsRepository
    ) {
        this.storeRepository = storeRepository;
        this.businessNumberRepository = businessNumberRepository;
        this.assignmentRepository = assignmentRepository;
        this.attendanceQrTokenRepository = attendanceQrTokenRepository;
        this.storeGpsRepository = storeGpsRepository;
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> getStoresForAdmin(String status, String q, Pageable pageable) {
        // Specification을 사용하여 동적 쿼리 생성
        Specification<Store> spec = (root, query, cb) -> {
            Predicate p = cb.conjunction();

            // 1. Status 필터링 (ALL이 아닐 경우)
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }

            // 2. q (검색어) 필터링 (storeName 기준)
            if (q != null && !q.trim().isEmpty()) {
                p = cb.and(p, cb.like(root.get("storeName"), "%" + q.trim() + "%"));
            }

            return p;
        };

    // 3. Specification과 Pageable을 사용하여 데이터 조회
        Page<Store> storePage = storeRepository.findAll(spec, pageable);

        // 4. Page<Store> -> Page<StoreResponse> 변환
        //    (StoreGps 정보 포함)
        return storePage.map(s -> {
            StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
            return StoreResponse.of(s, gps);
        });
    }

    public StoreResponse updateStoreStatus(Long storeId, String newStatus) {
        // "APPROVED" 또는 "REJECTED"만 허용 (이 로직은 유지)
        if (!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED")) {
            throw new IllegalArgumentException("잘못된 상태 값입니다. (APPROVED 또는 REJECTED만 가능)");
        }
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장을 찾을 수 없습니다."));

        // (선택적 개선) 이미 같은 상태로 변경하려 하면 경고
        if (store.getStatus().equals(newStatus)) {
            throw new IllegalStateException("이미 '" + newStatus + "' 상태입니다.");
        }

        store.setStatus(newStatus);
        Store updatedStore = storeRepository.save(store);
        
        return StoreResponse.from(updatedStore);
    }
    @Transactional(readOnly = true)
    public List<BusinessNumberResponse> getBusinessNumbersByOwner(Long ownerId) {
        List<BusinessNumber> bizList =
        businessNumberRepository.findByOwner_OwnerIdAndCertifiedAtIsNotNullAndOpenStatusNot(
                ownerId,
                "폐업자"
        );

        return bizList.stream()
                .map(BusinessNumberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 사업장 등록 (Admin/Owner 공용 사용 가능)
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

        Store saved = storeRepository.save(store);

        // ✅ 위치는 별도 테이블에 저장
        if (request.getLatitude() != null || request.getLongitude() != null) {
            StoreGps gps = new StoreGps();
            gps.setStore(saved);
            gps.setLatitude(request.getLatitude());
            gps.setLongitude(request.getLongitude());
            // 반경 기본값 있으면 여기서 세팅
            gps.setGpsRadiusM(80);
            storeGpsRepository.save(gps);
            return StoreResponse.of(saved, gps);
        }

        return StoreResponse.of(saved, null);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        return stores.stream()
                .map(s -> {
                    StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
                    return StoreResponse.of(s, gps);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);
        return StoreResponse.of(store, gps);
    }

    public StoreResponse updateStore(Long storeId, StoreCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장 없음"));

        store.setStoreName(request.getStoreName());
        store.setIndustry(request.getIndustry());
        store.setPosVendor(request.getPosVendor());

        if (request.getBizId() != null) {
            BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));
            store.setBusinessNumber(bn);
        }

        // ✅ GPS도 별도 처리
        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (gps == null) {
                gps = new StoreGps();
                gps.setStore(store);
            }
            gps.setLatitude(request.getLatitude());
            gps.setLongitude(request.getLongitude());
            if (gps.getGpsRadiusM() == null) {
                gps.setGpsRadiusM(80);
            }
            storeGpsRepository.save(gps);
        }

        return StoreResponse.of(store, gps);
    }

    public void deleteStore(Long storeId) {
        deleteStore(storeId, false);
    }

    public void deleteStore(Long storeId, boolean force) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));

        boolean hasChildren = assignmentRepository.existsByStore_StoreId(storeId);

            // ✅ 근무배정 / 직원 연결이 조금이라도 있으면 삭제 막기
        if (hasChildren) {
            throw new IllegalStateException(
                    "이 사업장에는 근무배정(직원 연결) 정보가 있어 삭제할 수 없습니다. " +
                    "근무 기록 보호를 위해 관리자에게 삭제를 요청해 주세요."
            );
        }

        // ✅ 연결이 전혀 없을 때만 삭제
        storeRepository.deleteById(storeId);
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

    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByOwner(Long ownerId) {
        // 기존에 쓰던 repo 메서드 유지
        List<Store> rows = storeRepository.findAllByOwnerId(ownerId);

        return rows.stream()
                .map(StoreSimpleResponse::from)
                .collect(Collectors.toList());
    }
}