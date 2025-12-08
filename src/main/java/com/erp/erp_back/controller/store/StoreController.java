package com.erp.erp_back.controller.store;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import com.erp.erp_back.dto.store.BusinessNumberResponse;
import com.erp.erp_back.dto.log.StoreQrResponse;
import com.erp.erp_back.dto.store.StoreCreateRequest;
import com.erp.erp_back.dto.store.StoreResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.service.store.StoreService;

@RestController
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.createStore(request));
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
      return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
      return ResponseEntity.ok(storeService.getStore(storeId));
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long storeId,
            @RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(storeService.updateStore(storeId, request));
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId,
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        storeService.deleteStore(storeId, force);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-owner/{ownerId}")
    public ResponseEntity<List<StoreSimpleResponse>> getStoresByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(storeService.getStoresByOwner(ownerId));
    }

    @GetMapping("/inactive/by-owner/{ownerId}")
    public ResponseEntity<List<StoreSimpleResponse>> getInactiveStoresByOwner(
        @PathVariable Long ownerId
    ) {
    return ResponseEntity.ok(storeService.getInactiveStoresByOwner(ownerId));
    }

     @PatchMapping("/{storeId}/activate")
    public ResponseEntity<Void> activateStore(@PathVariable Long storeId) {
        storeService.activateStore(storeId);
        return ResponseEntity.noContent().build();
    }

    // ✅ 새로 추가: ownerId 기준 사업자번호 목록 조회
    @GetMapping("/business-numbers/by-owner/{ownerId}")
    public ResponseEntity<List<BusinessNumberResponse>> getBusinessNumbersByOwner(
            @PathVariable Long ownerId
    ) {
        return ResponseEntity.ok(storeService.getBusinessNumbersByOwner(ownerId));
    }

    // ✅ QR 조회/재발급
    // 프론트: GET /api/store/11/qr?refresh=true
    @GetMapping("/{storeId}/qr")
    public ResponseEntity<StoreQrResponse> getOrRefreshQr(
            @PathVariable Long storeId,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh
    ) {
        return ResponseEntity.ok(storeService.getOrRefreshQr(storeId, refresh));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}