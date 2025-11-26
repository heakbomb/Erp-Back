package com.erp.erp_back.controller.user;

import com.erp.erp_back.entity.user.PaymentMethod;
import com.erp.erp_back.repository.user.PaymentMethodRepository;
import com.erp.erp_back.repository.user.OwnerRepository;
import com.erp.erp_back.entity.user.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Map<String, Object>>> getMyCards() {
        Long ownerId = 1L; // (토큰에서 추출 예정)
        
        List<Map<String, Object>> cards = paymentMethodRepository.findAllByOwner_OwnerId(ownerId)
            .stream()
            .map(pm -> Map.<String, Object>of( // ✅ 수정: 타입을 명시적으로 지정
                "paymentId", pm.getPaymentId(),
                "cardName", pm.getCardName() != null ? pm.getCardName() : "신용카드",
                "cardNumber", pm.getCardNumber() != null ? pm.getCardNumber() : "****",
                "isDefault", pm.isDefault()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(cards);
    }

    // 2. 카드 추가 (빌링키 등록)
    @PostMapping
    @Transactional
    public ResponseEntity<String> addCard(@RequestBody Map<String, String> request) {
        Long ownerId = 1L;
        String billingKey = request.get("customerUid");
        String cardName = request.getOrDefault("cardName", "새 카드");
        
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        PaymentMethod pm = new PaymentMethod();
        pm.setOwner(owner);
        pm.setBillingKey(billingKey);
        pm.setCardName(cardName);
        
        boolean hasDefault = paymentMethodRepository.findFirstByOwner_OwnerIdAndIsDefaultTrue(ownerId).isPresent();
        pm.setDefault(!hasDefault);

        paymentMethodRepository.save(pm);
        return ResponseEntity.ok("카드가 등록되었습니다.");
    }
}