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
            @Valid @RequestBody OwnerSubscriptionRequest request
    ) {
        // [로그 추가] 요청이 여기까지 들어오는지 확인
        System.out.println("==================================================");
        System.out.println("[DEBUG] createSubscription 요청 도착!");
        System.out.println("[DEBUG] SubId: " + request.getSubId());
        System.out.println("[DEBUG] CustomerUid(BillingKey): " + request.getCustomerUid());
        System.out.println("==================================================");
        Long tempOwnerId = 1L; 
        try {
            OwnerSubscriptionResponse response = ownerSubService.createSubscription(tempOwnerId, request);
            System.out.println("[DEBUG] 서비스 처리 완료: " + response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.out.println("[DEBUG] 에러 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    /**
     * ⭐️ [신규] (Owner) 현재 구독 상태 조회
     * GET /owner/subscriptions/current
     */
    @GetMapping("/current")
    public ResponseEntity<OwnerSubscriptionResponse> getCurrentSubscription() {
        Long tempOwnerId = 1L; // ⭐️ 사장 ID 1번 하드코딩
        
        // ⭐️ (참고) 서비스 레이어에 getSubscriptionByOwnerId 같은 메서드가 필요합니다.
        // 이 메서드는 OwnerSubscription과 Subscription을 조인(Join)해서
        // OwnerSubscriptionResponse DTO (subName, monthlyPrice 포함)를 반환해야 합니다.
        OwnerSubscriptionResponse response = ownerSubService.getCurrentSubscriptionByOwnerId(tempOwnerId);
        
        return ResponseEntity.ok(response);
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