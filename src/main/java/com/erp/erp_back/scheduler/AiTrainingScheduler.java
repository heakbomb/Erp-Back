package com.erp.erp_back.scheduler;

import com.erp.erp_back.service.ai.AiDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiTrainingScheduler {

    private final AiDataService aiDataService;

    // 매주 월요일 새벽 4시에 재학습 실행
    @Scheduled(cron = "0 0 4 * * MON", zone = "Asia/Seoul")
    public void runWeeklyRetraining() {
        System.out.println("[Scheduler] AI 주간 재학습 데이터 전송 시작");
        aiDataService.sendTrainingDataToPython();
    }
}