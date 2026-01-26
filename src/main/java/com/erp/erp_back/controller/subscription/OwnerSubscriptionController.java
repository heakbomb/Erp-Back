package com.erp.erp_back.controller.subscription;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import com.erp.erp_back.dto.subscription.OwnerSubscriptionRequest;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
import com.erp.erp_back.dto.subscription.SubscriptionResponse;
import com.erp.erp_back.service.subscription.OwnerSubscriptionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OwnerSubscriptionController {

    private final OwnerSubscriptionService ownerSubService;

    // 1. 구독 신청 API
    @PostMapping
    public ResponseEntity<OwnerSubscriptionResponse> createSubscription(
            @AuthenticationPrincipal String ownerIdStr, // 토큰에서 ID 받기
            @Valid @RequestBody OwnerSubscriptionRequest request
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        
        OwnerSubscriptionResponse response = ownerSubService.createSubscription(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. 사장님용 구독 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionProducts() {
        List<SubscriptionResponse> products = ownerSubService.getAllActiveSubscriptions();
        return ResponseEntity.ok(products);
    }

    // 3. 구독 해지 API
    @PostMapping("/{ownerSubId}/cancel")
    public ResponseEntity<String> cancelSubscription(
            @PathVariable Long ownerSubId,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.getOrDefault("reason", "단순 변심");
        ownerSubService.cancelSubscription(ownerSubId, reason);

        return ResponseEntity.ok("구독이 성공적으로 해지되었습니다. (다음 결제일부터 청구되지 않습니다.)");
    }

    // 4. [신규] 구독 해지 취소 API (500 에러 수정됨)
    @PostMapping("/{ownerSubId}/undo-cancel")
    public ResponseEntity<Void> undoCancelSubscription(
            Authentication authentication, // 변경: 안전하게 Authentication 전체를 받음
            @PathVariable Long ownerSubId) {
        
        Object principal = authentication.getPrincipal();
        Long ownerId;

        // Principal 타입에 따라 안전하게 ID 추출
        if (principal instanceof UserDetails) {
            // CustomUserDetails 객체인 경우 (getUsername()이 ID인 경우)
            ownerId = Long.parseLong(((UserDetails) principal).getUsername());
        } else if (principal instanceof String) {
            // String ID가 바로 들어오는 경우
            ownerId = Long.parseLong((String) principal);
        } else {
            // 그 외의 경우 (toString으로 시도)
            try {
                ownerId = Long.parseLong(principal.toString());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("지원하지 않는 사용자 인증 타입입니다: " + principal.getClass());
            }
        }
        
        ownerSubService.undoCancellation(ownerId, ownerSubId);
        
        return ResponseEntity.ok().build();
    }

    // 5. (Owner) 현재 구독 상태 조회
    @GetMapping("/current")
    public ResponseEntity<OwnerSubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal String ownerIdStr
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);

        OwnerSubscriptionResponse response = ownerSubService.getCurrentSubscriptionByOwnerId(ownerId);
        return ResponseEntity.ok(response);
    }

    // --- 예외 핸들러 ---
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