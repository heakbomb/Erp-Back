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
import com.erp.erp_back.entity.user.PaymentMethod;
import com.erp.erp_back.mapper.OwnerSubscriptionMapper;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.repository.subscripition.SubscriptionRepository;
import com.erp.erp_back.repository.user.OwnerRepository;
import com.erp.erp_back.repository.user.PaymentMethodRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OwnerSubscriptionService {

    private final OwnerSubscriptionRepository ownerSubRepo;
    private final OwnerRepository ownerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
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
            paymentMethodRepository.findByPaymentIdAndOwner_OwnerId(request.getPaymentMethodId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카드입니다."));
        }
        // [Case B] 새 카드 입력 시 (동시에 저장)
        else if (request.getCustomerUid() != null && !request.getCustomerUid().isBlank()) {
            saveOrUpdatePaymentMethod(owner, request.getCustomerUid(), request.getNewCardName());
        }

        // ✅ 2. 구독 정보 갱신 (UPSERT)
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElse(new OwnerSubscription());

        ownerSub.setOwner(owner);
        ownerSub.setSubscription(subscription);
        // 재구독 시 해지 상태 초기화
        ownerSub.setCanceled(false);
        ownerSub.setCancelReason(null);

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
     * 2. [신규] 구독 해지 (자동 결제 예약 취소)
     * - 만료일은 그대로 두고, canceled 플래그만 true로 변경
     * - 남은 기간 동안은 서비스 이용 가능
     */
    public void cancelSubscription(Long ownerSubId, String reason) {
        OwnerSubscription ownerSub = ownerSubRepo.findById(ownerSubId)
                .orElseThrow(() -> new EntityNotFoundException("구독 정보를 찾을 수 없습니다."));

        if (ownerSub.isCanceled()) {
            throw new IllegalStateException("이미 해지된 구독입니다.");
        }

        ownerSub.setCanceled(true);
        ownerSub.setCancelReason(reason);

        // 만약 PG사(포트원 등)에 '예약 결제'가 걸려있다면 여기서 API 호출하여 취소해야 함.
        // paymentModule.cancelSchedule(ownerSub.getOwner().getBillingKey());

        log.info("구독 해지 완료 - ID: {}, 사유: {}", ownerSubId, reason);
    }

    /**
     * 3. [신규] 자동 갱신 (스케줄러가 호출)
     * - 만료된 구독을 찾아 결제 시도 후 연장
     */
    public void renewSubscription(OwnerSubscription ownerSub) {
        Owner owner = ownerSub.getOwner();

        // 1. 기본 결제 수단(빌링키) 조회
        paymentMethodRepository.findFirstByOwner_OwnerIdAndIsDefaultTrue(owner.getOwnerId())
            .ifPresentOrElse(paymentMethod -> {
                // 2. [가상] PG사 결제 요청 (실제 연동 시 PortOne API 호출)
                boolean paymentSuccess = mockPaymentRequest(paymentMethod.getBillingKey(), ownerSub.getSubscription().getMonthlyPrice());

                if (paymentSuccess) {
                    // 3. 결제 성공 시: 만료일 1달 연장
                    ownerSub.setExpiryDate(ownerSub.getExpiryDate().plusMonths(1));
                    log.info("구독 갱신 성공 - Owner: {}, 만료일 연장: {}", owner.getUsername(), ownerSub.getExpiryDate());
                } else {
                    // 4. 결제 실패 시: 해지 처리
                    ownerSub.setCanceled(true);
                    ownerSub.setCancelReason("자동 결제 실패 (잔액 부족 등)");
                    log.warn("구독 갱신 실패 (결제 오류) - Owner: {}", owner.getUsername());
                }
            }, () -> {
                // 결제 수단 없음 -> 해지 처리
                ownerSub.setCanceled(true);
                ownerSub.setCancelReason("자동 결제 실패 (결제 수단 없음)");
                log.warn("구독 갱신 실패 (결제 수단 없음) - Owner: {}", owner.getUsername());
            });
    }

    // (내부용) 가짜 결제 모듈
    private boolean mockPaymentRequest(String billingKey, java.math.BigDecimal amount) {
        // 실제로는 여기서 PortOne API 호출
        return true; // 테스트를 위해 무조건 성공으로 가정
    }

    /**
     * 4. 카드(결제 수단)만 변경
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
        newMethod.setDefault(true);

        paymentMethodRepository.save(newMethod);
    }

    /**
     * 5. (Owner) 현재 구독 현황 조회
     */
    @Transactional(readOnly = true)
    public OwnerSubscriptionResponse getCurrentSubscriptionByOwnerId(Long ownerId) {
        OwnerSubscription ownerSub = ownerSubRepo.findFirstByOwner_OwnerIdOrderByExpiryDateDesc(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("현재 이용 중인 구독 정보가 없습니다."));

        return subscriptionMapper.toResponse(ownerSub);
    }

    /**
     * 6. (Admin) 전체 구독 현황 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminOwnerSubscriptionResponse> getAdminSubscriptions(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();
        Page<OwnerSubscription> page = ownerSubRepo.findAdminOwnerSubscriptions(effectiveQuery, pageable);
        return page.map(subscriptionMapper::toAdminResponse);
    }
}