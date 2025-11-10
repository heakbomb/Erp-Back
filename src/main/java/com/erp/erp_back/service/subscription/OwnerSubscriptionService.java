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
     * 1. (Owner) 구독 신청 또는 변경 (UPSERT)
     * 사장님(1L)의 기존 구독을 찾아서 덮어쓰거나(Update), 없으면 새로 생성(Create)합니다.
     */
    public OwnerSubscriptionResponse createSubscription(Long ownerId, OwnerSubscriptionRequest request) {
        
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        Subscription subscription = subscriptionRepository.findById(request.getSubId())
                .orElseThrow(() -> new EntityNotFoundException("구독 상품을 찾을 수 없습니다."));

        if (!subscription.isActive()) {
            throw new IllegalStateException("현재 신청할 수 없는 구독 상품입니다.");
        }

        // ❗️ [수정] 에러를 던지는 대신, '기존 구독'을 찾습니다.
        // (참고: Repository에 'findFirstByOwner_OwnerIdOrderByExpiryDateDesc'가 정의되어 있어야 합니다)
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElse(new OwnerSubscription()); // ⭐️ 기존 구독이 없으면 새 객체 생성

        // ❗️ [삭제] 기존 에러 발생 로직 삭제
        // ownerSubRepo.findFirstByOwnerOwnerIdAndExpiryDateAfter(ownerId, LocalDate.now())
        //     .ifPresent(existingSub -> {
        //         throw new IllegalStateException("이미 활성화된 구독(...)이 존재합니다.");
        //     });

        // [수정] 'newSub' -> 'ownerSub' (기존/신규 객체 공통 사용)
        ownerSub.setOwner(owner);
        ownerSub.setSubscription(subscription);
        ownerSub.setStartDate(LocalDate.now());
        ownerSub.setExpiryDate(LocalDate.now().plusMonths(1)); // ❗️ 정책: 1개월 구독

        OwnerSubscription saved = ownerSubRepo.save(ownerSub);
        
        // ⭐️ [수정] 'toDto' -> 'toFullDto' (Subscription 상세 정보 포함)
        return toFullDto(saved);
    }

    /**
     * ⭐️ [신규] 2. (Owner) 현재 구독 현황 조회 (GET /owner/subscriptions/current)
     * 사장님(1L)의 가장 최근 구독 정보를 DTO로 반환합니다.
     */
    @Transactional(readOnly = true) // ⭐️ Lazy-Loading을 위해 readOnly=true 필요
    public OwnerSubscriptionResponse getCurrentSubscriptionByOwnerId(Long ownerId) {
        
        // Repository에서 사장님의 가장 최근 구독 1건 조회
        // (참고: Repository에 'findFirstByOwner_OwnerIdOrderByExpiryDateDesc'가 정의되어 있어야 합니다)
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "사장님(ID: " + ownerId + ")의 구독 정보를 찾을 수 없습니다."
                ));

        // Full DTO로 변환하여 반환
        return toFullDto(ownerSub);
    }

    /**
     * 3. (Admin) 전체 구독 현황 목록 조회 (기존)
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
     * ⭐️ [수정] Entity -> OwnerSubscriptionResponse (Full DTO)
     * (기존 'toDto'를 대체하거나, 이름을 변경하여 사용)
     * (참고: OwnerSubscriptionResponse DTO에 subName, monthlyPrice 등이 있어야 합니다)
     */
    private OwnerSubscriptionResponse toFullDto(OwnerSubscription os) {
        Subscription sub = os.getSubscription(); // ⭐️ Lazy 로딩된 Subscription 정보 가져오기
        
        return OwnerSubscriptionResponse.builder()
                .ownerSubId(os.getOwnerSubId())
                .ownerId(os.getOwner().getOwnerId())
                .subId(sub.getSubId()) // ⭐️
                .startDate(os.getStartDate())
                .expiryDate(os.getExpiryDate())
                // ⭐️ [추가] 프론트엔드가 구독 현황 갱신에 필요한 상세 정보
                .subName(sub.getSubName())
                .monthlyPrice(sub.getMonthlyPrice())
                .isActive(sub.isActive())
                .build();
    }
}