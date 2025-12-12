package com.erp.erp_back.scheduler;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.repository.store.StoreRepository;
import com.erp.erp_back.service.hr.PayrollRunService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollFinalizeScheduler {

    private final StoreRepository storeRepository;
    private final PayrollRunService payrollRunService;

    /**
     * ✅ 매일 23:50 실행
     * - 오늘이 말일이면: 해당 월 FINALIZE 수행
     * - 말일이 아니면: 종료
     *
     * (Asia/Seoul 기준으로 서버 TZ가 맞아야 함)
     */
    @Scheduled(cron = "0 50 23 * * *")
    public void runAtEndOfDay() {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        LocalDate endOfMonth = ym.atEndOfMonth();

        // 말일이 아니면 아무것도 안 함 (스케줄러는 실행은 됨)
        if (!today.equals(endOfMonth)) {
            return;
        }

        // 모든 매장 대상
        List<Store> stores = storeRepository.findAll();

        for (Store s : stores) {
            Long storeId = s.getStoreId();
            try {
                payrollRunService.autoFinalizeIfNeeded(storeId, ym);
                log.info("[PayrollScheduler] finalized storeId={}, ym={}", storeId, ym);
            } catch (Exception e) {
                log.error("[PayrollScheduler] failed storeId={}, ym={}, err={}", storeId, ym, e.getMessage(), e);
            }
        }
    }
}