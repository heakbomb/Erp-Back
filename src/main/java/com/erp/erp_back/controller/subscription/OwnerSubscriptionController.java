package com.erp.erp_back.controller.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
@RequestMapping("/owner/subscriptions") // ✅ 사장님(Owner)용 API 루트
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OwnerSubscriptionController {

    private final OwnerSubscriptionService ownerSubService;

    /**
     * (Owner) 구독 신청
     * POST /owner/subscriptions
     */
    @PostMapping
    public ResponseEntity<OwnerSubscriptionResponse> createSubscription(
            // ❗️ [인증] 실제로는 @AuthenticationPrincipal Owner owner 로 받아야 함
            // ❗️ 임시로 ownerId=1L (기본 사장님) 사용
            @Valid @RequestBody OwnerSubscriptionRequest request
    ) {
        Long tempOwnerId = 1L; // ❗️ 임시 하드코딩된 사장님 ID
        
        OwnerSubscriptionResponse response = ownerSubService.createSubscription(tempOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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