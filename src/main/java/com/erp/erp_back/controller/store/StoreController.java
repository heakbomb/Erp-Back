package com.erp.erp_back.controller.store;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // ✅ 추가
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.service.store.StoreService;

@RestController
@RequestMapping("/api/store")
@CrossOrigin(origins = "*") // 프론트 연결시 CORS 방지
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // 사업장 등록
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.createStore(request));
    }

    // 전체 사업장 조회
    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    // 단일 사업장 조회
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    // 사업장 수정
    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long storeId,
            @RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.updateStore(storeId, request));
    }

    // ✅ 사업장 삭제 (force=true면 자식(배정/신청) 먼저 정리 후 삭제)
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId,
            @RequestParam(name = "force", defaultValue = "false") boolean force // ← 추가
    ) {
        // StoreService에 deleteStore(storeId, force) 가 구현되어 있어야 합니다.
        storeService.deleteStore(storeId, force);
        return ResponseEntity.noContent().build();
    }

    // ✅ 보기 좋은 에러 응답
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // ✅ 추가: 오너가 가진 모든 사업장 (사이드바용)
    // GET /api/store/by-owner/{ownerId}
    @GetMapping("/by-owner/{ownerId}")
    public ResponseEntity<List<StoreSimpleResponse>> getStoresByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(storeService.getStoresByOwner(ownerId));
    }
}