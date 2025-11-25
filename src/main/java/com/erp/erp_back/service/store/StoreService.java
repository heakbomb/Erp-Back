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
import com.erp.erp_back.mapper.StoreMapper; 
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.log.AttendanceQrTokenRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreGpsRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.dto.store.BusinessNumberResponse; 

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

        Page<Store> storePage = storeRepository.findAll(spec, pageable);

        // Mapper 사용: 목록 조회이므로 직원 상세(assignments)는 null 처리하여 성능 최적화
        return storePage.map(s -> {
            StoreGps gps = storeGpsRepository.findByStore_StoreId(s.getStoreId()).orElse(null);
            return storeMapper.toResponse(s, gps);
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
        
        // 상태 변경 후에는 상세 정보를 포함하여 반환
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

        // Mapper 사용: 모든 정보를 통합하여 반환
        return storeMapper.toResponse(store, gps, assignments);
    }

    /**
     * 매장 생성
     */
    public StoreResponse createStore(StoreCreateRequest request) {
        BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));

        // Builder 대신 Mapper 사용
        Store store = storeMapper.toEntity(request, bn);

        Store saved = storeRepository.save(store);

        // GPS 저장 로직 (별도 로직이므로 유지, 혹은 추후 GpsService로 분리 고려)
        StoreGps gps = null;
        if (request.getLatitude() != null || request.getLongitude() != null) {
            gps = new StoreGps();
            gps.setStore(saved);
            gps.setLatitude(request.getLatitude());
            gps.setLongitude(request.getLongitude());
            gps.setGpsRadiusM(80);
            storeGpsRepository.save(gps);
        }
        
        return storeMapper.toResponse(saved, gps);
    }

    /**
     * 매장 정보 수정
     */
    public StoreResponse updateStore(Long storeId, StoreCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장 없음"));

        // 일일이 setter 호출하는 대신 Mapper가 업데이트 수행
        storeMapper.updateFromDto(request, store);

        // 비즈니스 로직: 사업자 번호가 변경된 경우 별도 처리
        if (request.getBizId() != null && !request.getBizId().equals(store.getBusinessNumber().getBizId())) {
            BusinessNumber bn = businessNumberRepository.findById(request.getBizId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사업자(bizId) 입니다."));
            store.setBusinessNumber(bn);
        }

        // GPS 업데이트 로직
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
        
        return storeMapper.toResponse(store, gps);
    }

    public void deleteStore(Long storeId, boolean force) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));
        boolean hasChildren = assignmentRepository.existsByStore_StoreId(storeId);

            // ✅ 근무배정 / 직원 연결이 조금이라도 있으면 삭제 막기
        if (hasChildren && !force) {
            throw new IllegalStateException("해당 사업장에 연결된 근무 신청/배정이 있어 삭제할 수 없습니다.");
        }
        if (hasChildren) {
            throw new IllegalStateException(
                    "이 사업장에는 근무배정(직원 연결) 정보가 있어 삭제할 수 없습니다. " +
                    "근무 기록 보호를 위해 관리자에게 삭제를 요청해 주세요."
            );
        }
        attendanceQrTokenRepository.deleteByStore_StoreId(storeId);
        storeRepository.delete(store);
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
    public List<StoreSimpleResponse> getStoresByOwner(Long ownerId) {
        return storeRepository.findAllByOwnerId(ownerId).stream()
                .map(storeMapper::toSimpleResponse) // Mapper 사용 (수정됨)
                .collect(Collectors.toList());
    }

    public StoreQrResponse getOrRefreshQr(Long storeId, boolean refresh) {
        if (refresh) return regenerateQrToken(storeId);
        AttendanceQrToken latest = attendanceQrTokenRepository
                .findTopByStore_StoreIdOrderByExpireAtDesc(storeId).orElse(null);
        if (latest == null || latest.getExpireAt().isBefore(LocalDateTime.now())) {
            return regenerateQrToken(storeId);
        }
        // Mapper 사용 (수정됨)
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
        
        // Mapper 사용 (수정됨)
        return storeMapper.toQrResponse(savedQr);
    }

        // ⭐ ownerId 기준 사업자번호 목록 조회 (폐업자 제외 + 인증된 것만)
    @Transactional(readOnly = true)
    public List<BusinessNumberResponse> getBusinessNumbersByOwner(Long ownerId) {
        return businessNumberRepository
                .findByOwner_OwnerIdAndCertifiedAtIsNotNullAndOpenStatusNot(ownerId, "폐업자")
                .stream()
                .map(bn -> new BusinessNumberResponse(
                        bn.getBizId(),
                        (bn.getOwner() != null ? bn.getOwner().getOwnerId() : null),
                        bn.getPhone(),
                        bn.getBizNum()
                ))
                .collect(Collectors.toList());
    }

}