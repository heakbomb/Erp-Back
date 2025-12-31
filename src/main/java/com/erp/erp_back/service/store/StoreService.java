package com.erp.erp_back.service.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.log.StoreQrResponse;
import com.erp.erp_back.dto.store.BusinessNumberResponse;
import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreGpsResponse;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;
import com.erp.erp_back.mapper.StoreMapper;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreGpsRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import com.erp.erp_back.util.KmaGridConverter;
@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessNumberRepository businessNumberRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final AttendanceQrTokenRepository attendanceQrTokenRepository;
    private final StoreGpsRepository storeGpsRepository;

    private final StoreMapper storeMapper; // Mapper 주입

    /**
     * (Admin) 사업장 목록 조회 (페이징, 검색, 상태 필터링)
     */
    @Transactional(readOnly = true)
    public Page<StoreResponse> getStoresForAdmin(String status, String q, Pageable pageable) {
        Specification<Store> spec = (root, query, cb) -> {
            Predicate p = cb.conjunction();

            // 1. 상태 필터
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }

            // 2. 검색 로직
            if (q != null && !q.trim().isEmpty()) {
                String rawQ = q.trim();
                String numericQ = rawQ.replaceAll("[^0-9]", "");
                String likePatternRaw = "%" + rawQ + "%";
                String likePatternNum = "%" + numericQ + "%";

                Predicate nameLike = cb.like(root.get("storeName"), likePatternRaw);

                Join<Store, BusinessNumber> bizJoin = root.join("businessNumber", JoinType.LEFT);

                Predicate bizNumMatch;
                if (!numericQ.isEmpty()) {
                    Predicate matchNum = cb.like(bizJoin.get("bizNum"), likePatternNum);
                    Predicate matchRaw = cb.like(bizJoin.get("bizNum"), likePatternRaw);
                    bizNumMatch = cb.or(matchNum, matchRaw);
                } else {
                    bizNumMatch = cb.like(bizJoin.get("bizNum"), likePatternRaw);
                }

                p = cb.and(p, cb.or(nameLike, bizNumMatch));
            }
            return p;
        };

        // 2. Page<Store> 조회 (쿼리 1번)
        Page<Store> storePage = storeRepository.findAll(spec, pageable);

        // N+1 해결 로직
        // 2-1. 조회된 매장들의 ID 목록 추출
        List<Long> storeIds = storePage.getContent().stream()
                .map(Store::getStoreId)
                .toList();

        List<StoreGps> gpsList = storeGpsRepository.findAllByStore_StoreIdIn(storeIds);

        Map<Long, StoreGps> gpsMap = gpsList.stream()
                .collect(Collectors.toMap(
                        gps -> gps.getStore().getStoreId(),
                        gps -> gps));

        return storePage.map(s -> {
            StoreGps gps = gpsMap.get(s.getStoreId());
            return storeMapper.toResponse(s, gps);
        });
    }

    /**
     * (Admin) 사업장 상태 변경 (승인/반려)
     * - 여기서는 여전히 status 만 변경 (APPROVED / REJECTED / PENDING)
     * - active 는 건드리지 않음
     */
    public StoreResponse updateStoreStatus(Long storeId, String newStatus) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장을 찾을 수 없습니다."));

        // 1. 상태 변경
        store.setStatus(newStatus);

        // 2. 승인(APPROVED) 상태로 변경 시, 무조건 현재 시간으로 승인 일시 갱신
        if ("APPROVED".equals(newStatus)) {
            store.setApprovedAt(LocalDateTime.now());
        }

        Store updated = storeRepository.save(store);
        StoreGps gps = storeGpsRepository.findByStore_StoreId(updated.getStoreId()).orElse(null);

        List<EmployeeAssignment> assignments = assignmentRepository.findAllByStoreId(updated.getStoreId());
        return storeMapper.toResponse(updated, gps, assignments);
    }

    /**
     * (공용) 단건 상세 조회
     */
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));

        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);
        List<EmployeeAssignment> assignments = assignmentRepository.findAllByStoreId(storeId);

        return storeMapper.toResponse(store, gps, assignments);
    }

    /**
     * 매장 생성
     */
    public StoreResponse createStore(StoreCreateRequest request) {
        BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));

        // Mapper 에서 status / active 기본값 세팅 (예: status = PENDING, active = true)
        Store store = storeMapper.toEntity(request, bn);

        Store saved = storeRepository.save(store);

        // ⭐️ [수정] GPS 저장 로직 (필수값이므로 조건문 없이 무조건 저장)
        StoreGps gps = new StoreGps();
        gps.setStore(saved);
        gps.setLatitude(request.getLatitude());
        gps.setLongitude(request.getLongitude());

         // ✅ 추가: 위경도 → nx, ny 변환 저장 (로직 영향 없음)
        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(request.getLatitude(), request.getLongitude());
        gps.setNx(grid.nx());
        gps.setNy(grid.ny());

        // gpsRadiusM이 없으면 기본값 80 설정
        gps.setGpsRadiusM(request.getGpsRadiusM() != null ? request.getGpsRadiusM() : 80);
        storeGpsRepository.save(gps);

        return storeMapper.toResponse(saved, gps);
    }

    /**
     * 매장 정보 수정
     */
    public StoreResponse updateStore(Long storeId, StoreCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장 없음"));

        storeMapper.updateFromDto(request, store);

        // 사업자 번호 변경 시 처리
        if (request.getBizId() != null && !request.getBizId().equals(store.getBusinessNumber().getBizId())) {
            BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));
            store.setBusinessNumber(bn);
        }

        // ⭐️ [수정] GPS 업데이트 로직 (필수값이므로 무조건 처리)
        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId)
                .orElseGet(() -> {
                    StoreGps newGps = new StoreGps();
                    newGps.setStore(store);
                    return newGps;
                });

        gps.setLatitude(request.getLatitude());
        gps.setLongitude(request.getLongitude());

        // ✅ 추가: 위경도 → nx, ny 변환 저장 (로직 영향 없음)
        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(request.getLatitude(), request.getLongitude());
        gps.setNx(grid.nx());
        gps.setNy(grid.ny());

        if (request.getGpsRadiusM() != null) {
            gps.setGpsRadiusM(request.getGpsRadiusM());
        } else if (gps.getGpsRadiusM() == null) {
            gps.setGpsRadiusM(80); // 기존 값이 없거나 null로 들어오면 기본값 유지/설정
        }

        storeGpsRepository.save(gps);

        return storeMapper.toResponse(store, gps);
    }

    /**
     * ✅ 사장용 “비활성화” (Soft Delete)
     * - status 는 건드리지 않고 active 만 false 로 변경
     */
    public void deleteStore(Long storeId, boolean force) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));

        // 여기서는 일단 정책 단순화: 무조건 active=false 로만 처리
        store.setActive(Boolean.FALSE);

        storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll().stream()
                .map(s -> {
                    StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
                    return storeMapper.toResponse(s, gps);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByOwner(Long ownerId) {
        // 1) owner 기준으로 Store 목록 조회
        List<Store> stores = storeRepository.findAllByOwnerId(ownerId);

        // 2) 각 Store 에 대한 GPS 가져와서 StoreResponse 로 변환
        return stores.stream()
                .map(s -> {
                    StoreGps gps = storeGpsRepository
                            .findByStore_StoreId(s.getStoreId())
                            .orElse(null);
                    return storeMapper.toResponse(s, gps);
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ 사장용: “비활성화된 사업장 목록”
     * - status 가 아니라 active=false 기준으로 조회
     */
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getInactiveStoresByOwner(Long ownerId) {
        return storeRepository
                .findAllByBusinessNumber_Owner_OwnerIdAndActive(ownerId, false)
                .stream()
                .map(storeMapper::toSimpleResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 사장용: 비활성화된 사업장 다시 활성화
     * - status 는 그대로 (PENDING / APPROVED / REJECTED 값 유지)
     * - active 만 true 로 변경
     */
    public void activateStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사업장을 찾을 수 없습니다."));

        if (Boolean.FALSE.equals(store.getActive())) {
            store.setActive(Boolean.TRUE);
            storeRepository.save(store);
        }
    }

    /**
     * ✅ 출퇴근/QR 등에서 사용하는 “사용 가능한 사업장” 체크
     * - status 가 INACTIVE 인지 보던 기존 로직을 active=false 체크로 변경
     */
    @Transactional(readOnly = true)
    public Store requireActiveStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));

        // active 가 null 이면 기존 데이터 호환을 위해 true 로 간주
        if (Boolean.FALSE.equals(store.getActive())) {
            throw new IllegalStateException("비활성화된 사업장입니다. 활성화 후 이용해 주세요.");
        }

        return store;
    }

    public StoreQrResponse getOrRefreshQr(Long storeId, boolean refresh) {
        if (refresh)
            return regenerateQrToken(storeId);
        AttendanceQrToken latest = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(storeId).orElse(null);
        if (latest == null || latest.getExpireAt().isBefore(LocalDateTime.now())) {
            return regenerateQrToken(storeId);
        }
        return storeMapper.toQrResponse(latest);
    }

    private StoreQrResponse regenerateQrToken(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        attendanceQrTokenRepository.deleteByStore_StoreId(storeId);
        String token = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusMinutes(10);
        AttendanceQrToken qr = new AttendanceQrToken();
        qr.setStore(store);
        qr.setTokenValue(token);
        qr.setExpireAt(expires);

        AttendanceQrToken savedQr = attendanceQrTokenRepository.save(qr);

        return storeMapper.toQrResponse(savedQr);
    }

    // ⭐ ownerId 기준 사업자번호 목록 조회 (폐업자 제외 + 인증된 것만)
    @Transactional(readOnly = true)
    public List<BusinessNumberResponse> getBusinessNumbersByOwner(Long ownerId) {
        return businessNumberRepository
                .findByOwner_OwnerIdAndCertifiedAtIsNotNullAndOpenStatusNot(ownerId, "폐업자")
                .stream()
                .map(storeMapper::toBusinessNumberResponse)
                .collect(Collectors.toList());
    }

    // ⭐ 직원용: 승인된 활성 사업장 단건 조회
    @Transactional(readOnly = true)
    public StoreResponse getApprovedStoreForEmployee(Long storeId) {
        Store store = storeRepository.findApprovedActiveByStoreId(storeId)
                .orElseThrow(() -> new IllegalArgumentException("승인된 사업장만 조회할 수 있습니다."));

        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);
        // 직원 검색 화면에서 assignment까지 꼭 필요 없으면 빼도 되지만,
        // 다른 부분 건드리지 않는 선에서 기존 getStore 패턴 유지
        List<EmployeeAssignment> assignments = assignmentRepository.findAllByStoreId(storeId);

        return storeMapper.toResponse(store, gps, assignments);
    }
    // 직원 페이지 사업장 위치 정보 조회
    @Transactional(readOnly = true)
    public StoreGpsResponse getStoreGps(Long storeId) {
        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장의 위치 정보가 없습니다."));

        return new StoreGpsResponse(
                gps.getStore().getStoreId(),
                gps.getLatitude(),
                gps.getLongitude(),
                gps.getGpsRadiusM());
    }
}