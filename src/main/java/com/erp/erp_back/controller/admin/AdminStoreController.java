package com.erp.erp_back.controller.admin;

import java.util.Map;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.service.store.StoreService;
@RestController
@RequestMapping("/admin/stores") // 프론트와 일치하는 경로
@CrossOrigin(origins = "*")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final StoreService storeService;

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<Page<StoreResponse>> getAllStores( // ✅ 반환 타입 변경
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "storeId") Pageable pageable
    ) {
        // 이제 서비스가 DTO 페이지를 반환합니다.
        Page<StoreResponse> storeDtoPage = storeService.getStoresForAdmin(status, q, pageable);
        return ResponseEntity.ok(storeDtoPage);
    }

    /**
     * ✅ [수정] (Admin) 사업장 상태 변경 (반환 타입을 StoreResponse로 변경)
     */
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<StoreResponse> updateStoreStatus( // ✅ 반환 타입 변경
            @PathVariable Long storeId,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            throw new IllegalArgumentException("status 필드가 필요합니다.");
        }
        
        // 서비스가 DTO를 반환합니다.
        StoreResponse updatedStoreDto = storeService.updateStoreStatus(storeId, newStatus);
        return ResponseEntity.ok(updatedStoreDto);
    }
    
    /**
     * (Admin) 사업장 등록
     */
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.createStore(request));
    }

    /**
     * (Admin) 단일 사업장 조회
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    /**
     * (Admin) 사업장 수정
     */
    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long storeId,
            @RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.updateStore(storeId, request));
    }

    /**
     * (Admin) 사업장 삭제
     */
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId,
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        storeService.deleteStore(storeId, force);
        return ResponseEntity.noContent().build();
    }

    // (예외 핸들러는 동일하게 사용 가능)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}