package com.erp.erp_back.service.store;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
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
    private final EmployeeAssignmentRepository assignmentRepository; // ✅ 추가

    public StoreService(
            StoreRepository storeRepository,
            BusinessNumberRepository businessNumberRepository,
            EmployeeAssignmentRepository assignmentRepository // ✅ 추가
    ) {
        this.storeRepository = storeRepository;
        this.businessNumberRepository = businessNumberRepository;
        this.assignmentRepository = assignmentRepository; // ✅ 추가
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

        Store saved = storeRepository.save(store);
        return StoreResponse.from(saved);
    }

    // 전체 조회
    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사업장을 찾을 수 없습니다."));
        return StoreResponse.from(store);
    }

    // 수정
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

    // ❗ 기존 시그니처도 유지: 컨트롤러가 옛 메서드를 호출해도 안전하게 동작
    public void deleteStore(Long storeId) {
        deleteStore(storeId, false);
    }

    // ✅ FK 안전 삭제: force=false면 자식 존재 시 409 성격의 예외, force=true면 자식 먼저 제거
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
    // ✅ 추가: 오너별 사업장 단순 목록 (사이드바용)
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> getStoresByOwner(Long ownerId) {
        List<Store> rows = storeRepository.findAllByOwnerId(ownerId);
        return rows.stream()
                .map(StoreSimpleResponse::from)
                .collect(Collectors.toList());
    }
}