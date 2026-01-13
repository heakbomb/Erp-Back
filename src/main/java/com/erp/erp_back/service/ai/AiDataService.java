package com.erp.erp_back.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AiDataService {

    private final RestTemplate restTemplate;

    public void sendTrainingDataToPython() {
        String pythonUrl = "http://localhost:8000/train";
        
        try {
            System.out.println("🔔 [Java] Python AI 서버에 학습 요청 전송...");
            
            // 데이터 없이(null) 호출만 합니다. (Trigger)
            String response = restTemplate.postForObject(pythonUrl, null, String.class);
            
            System.out.println("🚀 [Java] 요청 성공! Python 응답: " + response);
            
        } catch (Exception e) {
            System.err.println("❌ [Java] Python 서버 연결 실패: " + e.getMessage());
        }
    }
}