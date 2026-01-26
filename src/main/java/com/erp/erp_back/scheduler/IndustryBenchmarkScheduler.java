// src/main/java/com/erp/erp_back/scheduler/IndustryBenchmarkScheduler.java
package com.erp.erp_back.scheduler;

import com.erp.erp_back.service.erp.IndustryBenchmarkBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndustryBenchmarkScheduler {

    private final IndustryBenchmarkBuildService buildService;

    @Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
    public void runLastMonth() {
        String ym = YearMonth.now().minusMonths(1).toString();
        log.info("[BENCH] build start ym={}", ym);
        buildService.buildMonthly(ym);
        log.info("[BENCH] build done ym={}", ym);
    }
}
