package com.erp.erp_back.service.subscription;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.subscription.SubscriptionRequest;
import com.erp.erp_back.dto.subscription.SubscriptionResponse;
import com.erp.erp_back.entity.subscripition.Subscription;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.repository.subscripition.SubscriptionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OwnerSubscriptionRepository ownerSubscriptionRepository;

    /**
     * (Admin) 구독 상품 목록 조회 (페이징, 검색, 필터)
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptions(String status, String q, Pageable pageable) {
        String effectiveStatus = (status == null || status.isEmpty()) ? "ALL" : status.toUpperCase();
        String effectiveQuery = (q == null) ? "" : q.trim();

        Page<Subscription> subPage = subscriptionRepository.findAdminSubscriptions(effectiveStatus, effectiveQuery, pageable);
        
        return subPage.map(this::toDto);
    }

    /**
     * (Admin) 구독 상품 단건 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long subId) {
        return subscriptionRepository.findById(subId)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다."));
    }

    /**
     * (Admin) 구독 상품 생성
     */
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setSubName(request.getSubName());
        subscription.setMonthlyPrice(request.getMonthlyPrice());
        subscription.setActive(request.getIsActive());
        
        Subscription saved = subscriptionRepository.save(subscription);
        return toDto(saved);
    }

    /**
     * (Admin) 구독 상품 수정
     */
    public SubscriptionResponse updateSubscription(Long subId, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다."));
        
        subscription.setSubName(request.getSubName());
        subscription.setMonthlyPrice(request.getMonthlyPrice());
        subscription.setActive(request.getIsActive());

        Subscription updated = subscriptionRepository.save(subscription);
        return toDto(updated);
    }

    /**
     * (Admin) 구독 상품 삭제
     */
    public void deleteSubscription(Long subId) {
        if (!subscriptionRepository.existsById(subId)) {
            throw new EntityNotFoundException("구독 상품을 찾을 수 없습니다.");
        }

        // ❗️ [중요] 이 상품을 구독 중인 활성 사용자가 있는지 확인
        boolean hasActiveUsers = ownerSubscriptionRepository
                .existsBySubscriptionSubIdAndExpiryDateAfter(subId, LocalDate.now());
        
        if (hasActiveUsers) {
            throw new IllegalStateException("이 구독 상품을 이용 중인 활성 사용자가 있어 삭제할 수 없습니다. 먼저 비활성(isActive=false)으로 변경하세요.");
        }

        subscriptionRepository.deleteById(subId);
    }

    /**
     * Entity -> DTO 변환
     */
    private SubscriptionResponse toDto(Subscription s) {
        return SubscriptionResponse.builder()
                .subId(s.getSubId())
                .subName(s.getSubName())
                .monthlyPrice(s.getMonthlyPrice())
                .isActive(s.isActive())
                .build();
    }
}