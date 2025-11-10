package com.erp.erp_back.service.store;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.auth.EmployeeAssignmentRepository;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.store.StoreRepository; // ✅ 추가
@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessNumberRepository businessNumberRepository;
    private final EmployeeAssignmentRepository assignmentRepository;

    public StoreService(
            StoreRepository storeRepository,
            BusinessNumberRepository businessNumberRepository,
            EmployeeAssignmentRepository assignmentRepository
    ) {
        this.storeRepository = storeRepository;
        this.businessNumberRepository = businessNumberRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> getStoresForAdmin(String status, String q, Pageable pageable) {
        String effectiveStatus = (status == null || status.equals("ALL")) ? "ALL" : status;
        String effectiveQuery = (q == null) ? "" : q;

        // 1. 엔티티 페이지 조회
        Page<Store> storePage = storeRepository.findAdminStores(effectiveStatus, effectiveQuery, pageable);
        
        // 2. ✅ 엔티티 페이지(Page<Store>)를 DTO 페이지(Page<StoreResponse>)로 변환
        // Page 객체의 .map() 기능을 사용합니다.
        return storePage.map(StoreResponse::from); // (StoreResponse.from() 메서드가 이미 있다고 가정)
    }

    public StoreResponse updateStoreStatus(Long storeId, String newStatus) {
        // "APPROVED" 또는 "REJECTED"만 허용 (이 로직은 유지)
        if (!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED")) {
            throw new IllegalArgumentException("잘못된 상태 값입니다. (APPROVED 또는 REJECTED만 가능)");
        }
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("사업장을 찾을 수 없습니다."));

        // ✅ [수정] "이미 처리된 사업장입니다." 예외 처리를 제거합니다.
        // (요청: 승인/반려 상태도 다시 변경할 수 있어야 함)
        /*
        if (!store.getStatus().equals("PENDING")) {
             throw new IllegalStateException("이미 처리된 사업장입니다.");
        }
        */

        // (선택적 개선) 이미 같은 상태로 변경하려 하면 경고
        if (store.getStatus().equals(newStatus)) {
            throw new IllegalStateException("이미 '" + newStatus + "' 상태입니다.");
        }

        store.setStatus(newStatus);
        Store updatedStore = storeRepository.save(store);
        
        return StoreResponse.from(updatedStore);
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
        return StoreResponse.from(saved);
    }

    // (Admin) 전체 조회
    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ✅ [신규 추가] (Admin) 사장 ID로 필터링 조회
     * AdminStoreController가 ownerId 파라미터와 함께 호출합니다.
     * (OwnerStoreController도 '본인 ID'를 넣어서 재사용 가능)
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByOwnerId(Long ownerId) {
        // 1. StoreRepository에 추가한 메서드를 호출합니다.
        List<Store> stores = storeRepository.findByBusinessNumber_Owner_OwnerId(ownerId);
        
        // 2. DTO로 변환하여 반환합니다.
        return stores.stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByStatus(String status) {
        // 1. StoreRepository에 추가한 메서드를 호출합니다.
        List<Store> stores = storeRepository.findByStatus(status);
        
        // 2. DTO로 변환하여 반환합니다.
        return stores.stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    // (Admin) 단건 조회 (Owner가 사용 시 Controller에서 소유권 검증 필요)
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        return StoreResponse.from(store);
    }

    // (Admin) 수정 (Owner가 사용 시 Controller에서 소유권 검증 필요)
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

        return StoreResponse.from(store);
    }

    // ❗ 기존 시그니처도 유지
    public void deleteStore(Long storeId) {
        deleteStore(storeId, false);
    }

    // (Admin) FK 안전 삭제 (Owner가 사용 시 Controller에서 force=false 강제)
    public void deleteStore(Long storeId, boolean force) {
        // 존재 확인
        storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 대상 사업장이 존재하지 않습니다."));

        boolean hasChildren = assignmentRepository.existsByStore_StoreId(storeId);

        if (hasChildren && !force) {
            // 컨트롤러의 @ExceptionHandler(IllegalStateException)에서 409로 내려가도록
            throw new IllegalStateException("해당 사업장에 연결된 근무 신청/배정이 있어 삭제할 수 없습니다. (force=true로 강제 삭제 가능)");
        }

        if (hasChildren) {
            // 자식 먼저 삭제
            assignmentRepository.deleteByStore_StoreId(storeId);
        }

        // 부모 삭제
        storeRepository.deleteById(storeId);
    }
}