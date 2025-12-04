package com.erp.erp_back.scheduler;

import com.erp.erp_back.service.erp.InventorySnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySnapshotScheduler {

    private final InventorySnapshotService inventorySnapshotService;

    /**
     * cron = "0 0 4 * * ?" (새벽 4시)
     * cron = "0 15 20 * * ?" (저녁 8시 15분)
     */
    @Scheduled(cron = "0 0 4 * * ?") 
    public void runDailyInventorySnapshot() {
        LocalDate targetDate = LocalDate.now().minusDays(1); 
        
        log.info("[Scheduler] 일별 재고 스냅샷 생성 시작 - 기준일: {}", targetDate);
        
        try {
            inventorySnapshotService.createSnapshotForDate(targetDate);
        } catch (Exception e) {
            log.error("[Scheduler] 재고 스냅샷 생성 중 오류 발생", e);
        }
    }
}