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
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.repository.subscripition.SubscriptionRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OwnerSubscriptionService {

    private final OwnerSubscriptionRepository ownerSubRepo;
    private final OwnerRepository ownerRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 1. (Owner) 구독 신청 (결제)
     * (임시: ownerId를 파라미터로 받지만, 실제로는 SecurityContext에서 가져와야 함)
     */
    public OwnerSubscriptionResponse createSubscription(Long ownerId, OwnerSubscriptionRequest request) {
        
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        Subscription subscription = subscriptionRepository.findById(request.getSubId())
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다."));

        if (!subscription.isActive()) {
            throw new IllegalStateException("현재 신청할 수 없는 구독 상품입니다.");
        }

        // ❗️ 이미 활성 구독이 있는지 확인 (Repository 쿼리 사용)
        ownerSubRepo.findFirstByOwnerOwnerIdAndExpiryDateAfter(ownerId, LocalDate.now())
            .ifPresent(existingSub -> {
                throw new IllegalStateException("이미 활성화된 구독(만료일: " + existingSub.getExpiryDate() + ")이 존재합니다.");
            });

        // (실제로는 결제 API 연동 후) 구독 정보 생성
        OwnerSubscription newSub = new OwnerSubscription();
        newSub.setOwner(owner);
        newSub.setSubscription(subscription);
        newSub.setStartDate(LocalDate.now());
        newSub.setExpiryDate(LocalDate.now().plusMonths(1)); // ❗️ 정책: 1개월 구독

        OwnerSubscription saved = ownerSubRepo.save(newSub);
        return toDto(saved);
    }

    /**
     * 2. (Admin) 전체 구독 현황 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminOwnerSubscriptionResponse> getAdminSubscriptions(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();

        // Repository의 findAdminOwnerSubscriptions 쿼리 호출
        Page<OwnerSubscription> page = ownerSubRepo.findAdminOwnerSubscriptions(effectiveQuery, pageable);
        
        // DTO로 변환
        return page.map(AdminOwnerSubscriptionResponse::from);
    }

    /**
     * Entity -> OwnerSubscriptionResponse (기본 DTO)
     */
    private OwnerSubscriptionResponse toDto(OwnerSubscription os) {
        return OwnerSubscriptionResponse.builder()
                .ownerSubId(os.getOwnerSubId())
                .ownerId(os.getOwner().getOwnerId())
                .subId(os.getSubscription().getSubId())
                .startDate(os.getStartDate())
                .expiryDate(os.getExpiryDate())
                .build();
    }
}