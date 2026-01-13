package com.erp.erp_back.controller.test;

import com.erp.erp_back.service.ai.AiDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
public class AiPipelineTestController {

    private final AiDataService aiDataService;

    // 개발자가 Postman이나 Swagger에서 이 API를 호출하면 즉시 데이터 전송 테스트 시작
    @PostMapping("/trigger-training")
    public ResponseEntity<String> triggerTrainingManually() {
        try {
            aiDataService.sendTrainingDataToPython();
            return ResponseEntity.ok("🚀 Python으로 학습 데이터 전송 요청 완료!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ 전송 실패: " + e.getMessage());
        }
    }
}