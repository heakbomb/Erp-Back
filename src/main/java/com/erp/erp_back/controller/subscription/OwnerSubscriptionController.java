package com.erp.erp_back.controller.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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

    // 구독 신청 API (기존 유지)
    @PostMapping
    public ResponseEntity<OwnerSubscriptionResponse> createSubscription(
            @Valid @RequestBody OwnerSubscriptionRequest request
    ) {
        // TODO: 추후 Spring Security의 @AuthenticationPrincipal 등으로 ownerId 교체 필요
        Long tempOwnerId = 1L; 
        OwnerSubscriptionResponse response = ownerSubService.createSubscription(tempOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // (Owner) 현재 구독 상태 조회 (DB 기반)
    @GetMapping("/current")
    public ResponseEntity<OwnerSubscriptionResponse> getCurrentSubscription() {
        Long tempOwnerId = 1L; 

        // 서비스 -> 리포지토리(Join Fetch) -> DB 조회 -> DTO 변환(가격/이름 포함)
        OwnerSubscriptionResponse response = ownerSubService.getCurrentSubscriptionByOwnerId(tempOwnerId);
        
        return ResponseEntity.ok(response);
    }                                           
             
    // --- 예외 핸들러 ---
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        // 구독 정보가 없을 때 404 리턴
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