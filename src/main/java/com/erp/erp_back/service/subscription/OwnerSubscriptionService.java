package com.erp.erp_back.service.subscription;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.subscription.AdminOwnerSubscriptionResponse;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionRequest;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
import com.erp.erp_back.entity.subscripition.OwnerSubscription;
import com.erp.erp_back.entity.subscripition.Subscription;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.entity.user.PaymentMethod; // ✅ 추가
import com.erp.erp_back.mapper.OwnerSubscriptionMapper;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.repository.subscripition.SubscriptionRepository;
import com.erp.erp_back.repository.user.OwnerRepository;
import com.erp.erp_back.repository.user.PaymentMethodRepository; // ✅ 추가

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OwnerSubscriptionService {

    private final OwnerSubscriptionRepository ownerSubRepo;
    private final OwnerRepository ownerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentMethodRepository paymentMethodRepository; // ✅ 추가
    private final OwnerSubscriptionMapper subscriptionMapper;

    /**
     * 1. (Owner) 구독 신청 및 결제 수단 처리
     */
    public OwnerSubscriptionResponse createSubscription(Long ownerId, OwnerSubscriptionRequest request) {

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Subscription subscription = subscriptionRepository.findById(request.getSubId())
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다."));

        if (!subscription.isActive()) {
            throw new IllegalStateException("현재 신청할 수 없는 구독 상품입니다.");
        }

        // ✅ 1. 결제 수단 처리 로직
        // [Case A] 기존 카드 선택 시
        if (request.getPaymentMethodId() != null) {
            // 변수 할당 없이 조회만 수행 (없으면 예외 발생) -> 검증 완료
            paymentMethodRepository.findByPaymentIdAndOwner_OwnerId(request.getPaymentMethodId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카드입니다."));
        }
        // [Case B] 새 카드 입력 시 (동시에 저장)
        else if (request.getCustomerUid() != null && !request.getCustomerUid().isBlank()) {
            saveOrUpdatePaymentMethod(owner, request.getCustomerUid(), request.getNewCardName());
        }
        // [Case C] 결제 수단 없이 요청한 경우 (무료 상품 등 특수 케이스가 아니면 예외)
        else {
            // throw new IllegalArgumentException("결제 수단이 선택되지 않았습니다.");
        }

        // ✅ 2. 구독 정보 갱신 (UPSERT)
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElse(new OwnerSubscription());

        ownerSub.setOwner(owner);
        ownerSub.setSubscription(subscription);

        // 날짜 계산: 이미 구독 중이면 [기존 만료일 + 1달], 아니면 [오늘 + 1달]
        LocalDate today = LocalDate.now();
        if (ownerSub.getExpiryDate() != null && ownerSub.getExpiryDate().isAfter(today)) {
            ownerSub.setExpiryDate(ownerSub.getExpiryDate().plusMonths(1));
        } else {
            ownerSub.setStartDate(today);
            ownerSub.setExpiryDate(today.plusMonths(1));
        }

        OwnerSubscription saved = ownerSubRepo.save(ownerSub);
        return subscriptionMapper.toResponse(saved);
    }

    /**
     * ⭐️ [신규] 카드(결제 수단)만 변경 (기간 연장 X)
     */
    public void updatePaymentMethodOnly(Long ownerId, String newCustomerUid) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        saveOrUpdatePaymentMethod(owner, newCustomerUid, "변경된 카드");
    }

    /**
     * 헬퍼 메서드: 기존 카드를 내리고 새 카드를 '기본(Default)'으로 등록
     */
    private void saveOrUpdatePaymentMethod(Owner owner, String billingKey, String cardName) {
        // 1. 기존에 기본으로 설정된 카드가 있다면 해제
        paymentMethodRepository.findFirstByOwner_OwnerIdAndIsDefaultTrue(owner.getOwnerId())
                .ifPresent(method -> {
                    method.setDefault(false);
                });

        // 2. 새 결제 수단 생성 및 저장
        PaymentMethod newMethod = new PaymentMethod();
        newMethod.setOwner(owner);
        newMethod.setBillingKey(billingKey);
        newMethod.setProvider("PORTONE");
        newMethod.setCardName(cardName != null ? cardName : "새 카드");
        newMethod.setDefault(true); // 방금 등록했으니 이게 기본 카드

        paymentMethodRepository.save(newMethod);
    }

    // ==========================================
    // ⬇️ 아래 메서드들이 없어서 에러가 났던 것입니다. ⬇️
    // ==========================================

    /**
     * 2. (Owner) 현재 구독 현황 조회
     */
    @Transactional(readOnly = true)
    public OwnerSubscriptionResponse getCurrentSubscriptionByOwnerId(Long ownerId) {
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("구독 정보를 찾을 수 없습니다."));
        return subscriptionMapper.toResponse(ownerSub);
    }

    /**
     * 3. (Admin) 전체 구독 현황 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminOwnerSubscriptionResponse> getAdminSubscriptions(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();
        Page<OwnerSubscription> page = ownerSubRepo.findAdminOwnerSubscriptions(effectiveQuery, pageable);
        return page.map(subscriptionMapper::toAdminResponse);
    }
}