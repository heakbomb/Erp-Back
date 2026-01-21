package com.erp.erp_back.controller.user;

import com.erp.erp_back.entity.user.PaymentMethod;
import com.erp.erp_back.repository.user.PaymentMethodRepository;
import com.erp.erp_back.repository.user.OwnerRepository;
import com.erp.erp_back.entity.user.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ✅ 추가
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/owner/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;
    private final OwnerRepository ownerRepository;

    // 1. 내 카드 목록 조회
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyCards(
            @AuthenticationPrincipal String ownerIdStr // ✅ 수정: 로그인 정보 사용
    ) {
        Long ownerId = Long.parseLong(ownerIdStr); // String -> Long 변환
        
        List<Map<String, Object>> cards = paymentMethodRepository.findAllByOwner_OwnerId(ownerId)
            .stream()
            .map(pm -> Map.<String, Object>of(
                "paymentId", pm.getPaymentId(),
                "cardName", pm.getCardName() != null ? pm.getCardName() : "신용카드",
                "isDefault", pm.isDefault()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(cards);
    }

    // 2. 카드 추가 (빌링키 저장)
    @PostMapping
    @Transactional
    public ResponseEntity<String> addCard(
            @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 로그인 정보 사용
            @RequestBody Map<String, String> request
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        String billingKey = request.get("customerUid");
        String cardName = request.getOrDefault("cardName", "새 카드");
        
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        PaymentMethod pm = new PaymentMethod();
        pm.setOwner(owner);
        pm.setBillingKey(billingKey);
        pm.setCardName(cardName);
        pm.setProvider("PORTONE"); 
        
        boolean hasDefault = paymentMethodRepository.findFirstByOwner_OwnerIdAndIsDefaultTrue(ownerId).isPresent();
        pm.setDefault(!hasDefault);

        paymentMethodRepository.save(pm);
        return ResponseEntity.ok("카드가 등록되었습니다.");
    }

    // 3. 카드 이름(별칭) 수정
    @PutMapping("/{paymentId}")
    @Transactional
    public ResponseEntity<String> updateCardName(
        @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 로그인 정보 사용
        @PathVariable Long paymentId,
        @RequestBody Map<String, String> request
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);
        String newName = request.get("cardName");

        PaymentMethod pm = paymentMethodRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        // 내 카드가 맞는지 확인
        if (!pm.getOwner().getOwnerId().equals(ownerId)) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        pm.setCardName(newName);
        paymentMethodRepository.save(pm);

        return ResponseEntity.ok("카드 이름이 변경되었습니다.");
    }

    // 4. 카드 삭제
    @DeleteMapping("/{paymentId}")
    @Transactional
    public ResponseEntity<String> deleteCard(
        @AuthenticationPrincipal String ownerIdStr, // ✅ 수정: 로그인 정보 사용
        @PathVariable Long paymentId
    ) {
        Long ownerId = Long.parseLong(ownerIdStr);

        PaymentMethod pm = paymentMethodRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        // 내 카드가 맞는지 확인
        if (!pm.getOwner().getOwnerId().equals(ownerId)) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        paymentMethodRepository.delete(pm);

        return ResponseEntity.ok("카드가 삭제되었습니다.");
    }
}