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
import com.erp.erp_back.entity.auth.EmployeeAssignment;
import com.erp.erp_back.entity.log.AttendanceQrToken;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.store.StoreGps;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreGpsRepository;
import com.erp.erp_back.repository.store.StoreRepository;

// ⭐️ 검색 기능을 위한 Import 추가
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessNumberRepository businessNumberRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final AttendanceQrTokenRepository attendanceQrTokenRepository;
    private final StoreGpsRepository storeGpsRepository;

    /**
     * (Admin) 사업장 목록 조회 (페이징, 검색, 상태 필터링)
     * - 검색 강화: 사업장명 OR (사업자번호 숫자만) OR (사업자번호 원본)
     */
    @Transactional(readOnly = true)
    public Page<StoreResponse> getStoresForAdmin(String status, String q, Pageable pageable) {
        Specification<Store> spec = (root, query, cb) -> {
            Predicate p = cb.conjunction();

            // 1. 상태 필터
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }

            // 2. 검색 로직 (사업장명 OR 사업자번호)
            if (q != null && !q.trim().isEmpty()) {
                String rawQ = q.trim(); // 입력값 그대로
                String numericQ = rawQ.replaceAll("[^0-9]", ""); // 숫자만 추출
                String likePatternRaw = "%" + rawQ + "%";
                String likePatternNum = "%" + numericQ + "%";

                // 이름 검색
                Predicate nameLike = cb.like(root.get("storeName"), likePatternRaw);
                
                // 사업자번호 검색을 위한 조인
                Join<Store, BusinessNumber> bizJoin = root.join("businessNumber", JoinType.LEFT);

                Predicate bizNumMatch;
                if (!numericQ.isEmpty()) {
                    // 숫자가 있으면: (하이픈 없는 값) OR (하이픈 있는 값) 모두 검색
                    Predicate matchNum = cb.like(bizJoin.get("bizNum"), likePatternNum);
                    Predicate matchRaw = cb.like(bizJoin.get("bizNum"), likePatternRaw);
                    bizNumMatch = cb.or(matchNum, matchRaw);
                } else {
                    // 문자만 있으면: 원본으로만 검색
                    bizNumMatch = cb.like(bizJoin.get("bizNum"), likePatternRaw);
                }
                
                p = cb.and(p, cb.or(nameLike, bizNumMatch));
            }

            return p;
        };

        Page<Store> storePage = storeRepository.findAll(spec, pageable);

        // DTO 변환 (목록에서도 사업자 번호 표시)
        return storePage.map(s -> {
            StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
            StoreResponse resp = StoreResponse.of(s, gps);
            if (s.getBusinessNumber() != null) {
                resp.setBizNum(s.getBusinessNumber().getBizNum());
            }
            return resp;
        });
    }

    /**
     * (Admin) 사업장 상태 변경 (승인/반려)
     */
    public StoreResponse updateStoreStatus(Long storeId, String newStatus) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장을 찾을 수 없습니다."));

        store.setStatus(newStatus);
        
        if ("APPROVED".equals(newStatus) && store.getApprovedAt() == null) {
            store.setApprovedAt(LocalDateTime.now());
        }

        Store updated = storeRepository.save(store);
        StoreGps gps = storeGpsRepository.findByStore_StoreId(updated.getStoreId()).orElse(null);
        
        // 상세 정보 포함 반환
        return getStoreResponseWithDetails(updated, gps);
    }

    /**
     * (공용) 단건 상세 조회
     */
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        
        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);

        // 상세 정보 포함 반환
        return getStoreResponseWithDetails(store, gps);
    }

    // ⭐️ [헬퍼 메서드] 상세 정보(사장님, 직원 등) 채우기
    private StoreResponse getStoreResponseWithDetails(Store store, StoreGps gps) {
        StoreResponse response = StoreResponse.of(store, gps);

        // 1. 사업자 및 사장님 정보
        if (store.getBusinessNumber() != null) {
            BusinessNumber biz = store.getBusinessNumber();
            response.setBizNum(biz.getBizNum());
            response.setPhone(biz.getPhone());
            response.setOpenStatus(biz.getOpenStatus());
            response.setTaxType(biz.getTaxType());
            response.setStartDt(biz.getStartDt());
            
            if (biz.getOwner() != null) {
                response.setOwnerName(biz.getOwner().getUsername());
                response.setOwnerEmail(biz.getOwner().getEmail());
            }
        }

        // 2. 직원 목록 조회
        List<EmployeeAssignment> assignments = assignmentRepository.findAllByStoreId(store.getStoreId());
        List<StoreResponse.StoreEmployeeDto> employeeDtos = assignments.stream()
                .map(ea -> StoreResponse.StoreEmployeeDto.builder()
                        .name(ea.getEmployee().getName())
                        .role(ea.getRole())
                        .status(ea.getStatus())
                        .build())
                .collect(Collectors.toList());
        
        response.setEmployees(employeeDtos);

        return response;
    }

    // ---------------------------------------------------------
    // 기존 메서드들 (유지)
    // ---------------------------------------------------------

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

        StoreGps gps = null;
        if (request.getLatitude() != null || request.getLongitude() != null) {
            gps = new StoreGps();
            gps.setStore(saved);
            gps.setLatitude(request.getLatitude());
            gps.setLongitude(request.getLongitude());
            gps.setGpsRadiusM(80);
            storeGpsRepository.save(gps);
        }
        return StoreResponse.of(saved, gps);
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

        StoreGps gps = storeGpsRepository.findByStore_StoreId(storeId).orElse(null);
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (gps == null) {
                gps = new StoreGps();
                gps.setStore(store);
            }
            gps.setLatitude(request.getLatitude());
            gps.setLongitude(request.getLongitude());
            if (gps.getGpsRadiusM() == null) gps.setGpsRadiusM(80);
            storeGpsRepository.save(gps);
        }
        return StoreResponse.of(store, gps);
    }

    public void deleteStore(Long storeId, boolean force) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));
        boolean hasChildren = assignmentRepository.existsByStore_StoreId(storeId);
        if (hasChildren && !force) {
            throw new IllegalStateException("해당 사업장에 연결된 근무 신청/배정이 있어 삭제할 수 없습니다.");
        }
        if (hasChildren) {
            assignmentRepository.deleteByStore_StoreId(storeId);
        }
        attendanceQrTokenRepository.deleteByStore_StoreId(storeId);
        storeRepository.delete(store);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll().stream()
                .map(s -> {
                    StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
                    return StoreResponse.of(s, gps);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByOwner(Long ownerId) {
        return storeRepository.findAllByOwnerId(ownerId).stream()
                .map(StoreSimpleResponse::from)
                .collect(Collectors.toList());
    }

    public StoreQrResponse getOrRefreshQr(Long storeId, boolean refresh) {
        if (refresh) return regenerateQrToken(storeId);
        AttendanceQrToken latest = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(storeId).orElse(null);
        if (latest == null || latest.getExpireAt().isBefore(LocalDateTime.now())) {
            return regenerateQrToken(storeId);
        }
        return StoreQrResponse.builder()
                .storeId(storeId)
                .qrToken(latest.getTokenValue())
                .expireAt(latest.getExpireAt())
                .build();
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
        attendanceQrTokenRepository.save(qr);
        return StoreQrResponse.builder()
                .storeId(storeId)
                .qrToken(token)
                .expireAt(expires)
                .build();
    }
}