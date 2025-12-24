package com.erp.erp_back.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.subscripition.OwnerSubscription;
import com.erp.erp_back.repository.subscripition.OwnerSubscriptionRepository;
import com.erp.erp_back.service.subscription.OwnerSubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalScheduler {

    private final OwnerSubscriptionRepository ownerSubRepo;
    private final OwnerSubscriptionService ownerSubService;

    // 매일 새벽 0시 0분 0초에 실행
    @Scheduled(cron = "0 0 0 * * *") 
    @Transactional
    public void autoRenewSubscriptions() {
        LocalDate today = LocalDate.now();
        log.info("🔄 [자동 결제] 스케줄러 시작 - 기준일: {}", today);

        // 1. 오늘 만료되면서, 해지 신청을 안 한(canceled=false) 구독 조회
        // (주의: 어제 만료된 걸 오늘 갱신할지, 오늘 만료되는 걸 갱신할지는 정책 나름. 여기선 '오늘 만료' 대상)
        List<OwnerSubscription> expiringSubs = ownerSubRepo.findByExpiryDateAndCanceledFalse(today);

        log.info("대상 구독 수: {}건", expiringSubs.size());

        // 2. 각 구독에 대해 연장(결제) 시도
        for (OwnerSubscription sub : expiringSubs) {
            try {
                ownerSubService.renewSubscription(sub);
            } catch (Exception e) {
                log.error("구독 ID {} 갱신 중 오류 발생", sub.getOwnerSubId(), e);
            }
        }
        
        log.info("✅ [자동 결제] 스케줄러 종료");
    }
}