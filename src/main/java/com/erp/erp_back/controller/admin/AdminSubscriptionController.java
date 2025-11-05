package com.erp.erp_back.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.subscription.SubscriptionRequest;
import com.erp.erp_back.dto.subscription.SubscriptionResponse;
import com.erp.erp_back.service.subscription.SubscriptionService; // ❗️ 이 파일이 필요합니다.

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/subscriptions") // ✅ 프론트엔드가 호출한 바로 그 경로
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * (Admin) 구독 상품 목록 조회 (페이징, 검색, 필터)
     * GET /admin/subscriptions?status=ACTIVE&q=프리미엄&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<SubscriptionResponse>> getSubscriptions(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "subId") Pageable pageable
    ) {
        Page<SubscriptionResponse> subPage = subscriptionService.getSubscriptions(status, q, pageable);
        return ResponseEntity.ok(subPage);
    }

    /**
     * (Admin) 구독 상품 단건 조회
     * GET /admin/subscriptions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    /**
     * (Admin) 구독 상품 생성
     * POST /admin/subscriptions
     */
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscriptionRequest request
    ) {
        SubscriptionResponse created = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * (Admin) 구독 상품 수정
     * PUT /admin/subscriptions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request
    ) {
        SubscriptionResponse updated = subscriptionService.updateSubscription(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * (Admin) 구독 상품 삭제
     * DELETE /admin/subscriptions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    // --- (예외 핸들러) ---
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}