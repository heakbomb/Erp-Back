package com.erp.erp_back.service.subscription;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 1. (Admin) 구독 상품 목록 조회 (GET /admin/subscriptions)
     * ⭐️ Repository의 @Query 메서드를 호출합니다.
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptions(String status, String q, Pageable pageable) {
        // ⭐️ 제공해주신 Repository의 findAdminSubscriptions 쿼리 호출
        Page<Subscription> subPage = subscriptionRepository.findAdminSubscriptions(status, q, pageable);
        
        // ⭐️ Entity Page -> DTO Page 변환
        return subPage.map(SubscriptionResponse::fromEntity);
    }

    /**
     * 2. (Admin) 구독 상품 단건 조회 (GET /admin/subscriptions/{id})
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long id) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다. ID: " + id));
        return SubscriptionResponse.fromEntity(sub);
    }

    /**
     * 3. (Admin) 구독 상품 생성 (POST /admin/subscriptions)
     */
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        Subscription newSub = new Subscription();
        newSub.setSubName(request.getSubName());
        newSub.setMonthlyPrice(request.getMonthlyPrice());
        newSub.setActive(request.getIsActive());
        
        Subscription saved = subscriptionRepository.save(newSub);
        return SubscriptionResponse.fromEntity(saved);
    }

    /**
     * 4. (Admin) 구독 상품 수정 (PUT /admin/subscriptions/{id})
     */
    @Transactional
    public SubscriptionResponse updateSubscription(Long id, SubscriptionRequest request) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다. ID: " + id));
        
        sub.setSubName(request.getSubName());
        sub.setMonthlyPrice(request.getMonthlyPrice());
        sub.setActive(request.getIsActive());
        
        Subscription updated = subscriptionRepository.save(sub);
        return SubscriptionResponse.fromEntity(updated);
    }

    /**
     * 5. (Admin) 구독 상품 삭제 (DELETE /admin/subscriptions/{id})
     */
    @Transactional
    public void deleteSubscription(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new EntityNotFoundException("삭제할 구독 상품을 찾을 수 없습니다. ID: " + id);
        }
        // ⭐️ (주의) 이 상품을 구독 중인 사장님이 있으면 오류가 날 수 있습니다.
        // (향후 비활성화(isActive=false) 처리로 변경하는 것을 권장)
        subscriptionRepository.deleteById(id);
    }
}