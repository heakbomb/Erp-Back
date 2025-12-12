package com.erp.erp_back.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesSummaryStartupRunner {

    private final SalesSummaryScheduler salesSummaryScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("[Startup] 앱 시작 시 최근 매출 요약 백필 시작");
        salesSummaryScheduler.summarizeYesterdayData();  // ✅ 서버 켜질 때도 한 번 실행
        log.info("[Startup] 앱 시작 시 최근 매출 요약 백필 완료");
    }
}
