package com.erp.erp_back.controller.subscription;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.subscription.OwnerSubscriptionRequest;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
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

    // 구독 신청 API
    @PostMapping
    public ResponseEntity<OwnerSubscriptionResponse> createSubscription(
            @Valid @RequestBody OwnerSubscriptionRequest request
    ) {
        // 추후 Spring Security의 @AuthenticationPrincipal 등으로 ownerId 교체 필요
        Long tempOwnerId = 1L;
        OwnerSubscriptionResponse response = ownerSubService.createSubscription(tempOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // [신규] 구독 해지 API
    // POST /owner/subscriptions/{id}/cancel
    @PostMapping("/{ownerSubId}/cancel")
    public ResponseEntity<String> cancelSubscription(
            @PathVariable Long ownerSubId,
            @RequestBody Map<String, String> body // { "reason": "..." }
    ) {
        String reason = body.getOrDefault("reason", "단순 변심");
        ownerSubService.cancelSubscription(ownerSubId, reason);

        return ResponseEntity.ok("구독이 성공적으로 해지되었습니다. (다음 결제일부터 청구되지 않습니다.)");
    }

    // (Owner) 현재 구독 상태 조회
    @GetMapping("/current")
    public ResponseEntity<OwnerSubscriptionResponse> getCurrentSubscription() {
        Long tempOwnerId = 1L;

        OwnerSubscriptionResponse response = ownerSubService.getCurrentSubscriptionByOwnerId(tempOwnerId);
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