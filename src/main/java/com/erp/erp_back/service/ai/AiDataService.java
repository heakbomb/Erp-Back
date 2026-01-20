package com.erp.erp_back.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.erp.erp_back.dto.ai.DemandForecastResponse;
import com.erp.erp_back.entity.ai.DemandForecast;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.repository.ai.DemandForecastRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDataService {

    private final DemandForecastRepository demandForecastRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestClient restClient; // RestClientConfig에 등록된 빈 사용

    // ✅ [추가] 파이썬 AI 서버로 학습 요청 보내기
    public String sendTrainingDataToPython() {
        try {
            log.info("🚀 AI 학습 요청 전송 중... (POST http://localhost:8000/train)");
            
            String response = restClient.post()
                    .uri("http://localhost:8000/train")
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            
            log.info("✅ AI 학습 요청 성공: {}", response);
            return response;
        } catch (Exception e) {
            log.error("❌ AI 학습 요청 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 연결 실패: " + e.getMessage());
        }
    }

    // (기존) 주간 수요 예측 조회 로직 유지
    public List<DemandForecastResponse> getWeeklyForecast(Long storeId) {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(6);

        // 1. 해당 기간의 메뉴별 예측 데이터 조회
        List<DemandForecast> forecasts = demandForecastRepository.findByStoreIdAndTargetDateBetween(storeId, startDate, endDate);

        // 2. 메뉴 가격 정보 조회
        List<Long> menuIds = forecasts.stream().map(DemandForecast::getMenuId).distinct().collect(Collectors.toList());
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuIds);
        Map<Long, BigDecimal> priceMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getMenuId, MenuItem::getPrice));

        // 3. 날짜별 합계 계산
        Map<LocalDate, BigDecimal> dailyTotalSales = new HashMap<>();
        Map<LocalDate, Integer> dailyTotalQty = new HashMap<>();

        for (DemandForecast f : forecasts) {
            LocalDate date = f.getTargetDate();
            BigDecimal price = priceMap.getOrDefault(f.getMenuId(), BigDecimal.ZERO);
            BigDecimal sales = price.multiply(BigDecimal.valueOf(f.getPredictedQty()));

            dailyTotalSales.merge(date, sales, BigDecimal::add);
            dailyTotalQty.merge(date, f.getPredictedQty(), Integer::sum);
        }

        // 4. 결과 변환
        List<DemandForecastResponse> responseList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal totalSales = dailyTotalSales.getOrDefault(date, BigDecimal.ZERO);
            int estimatedVisitors = (int) (dailyTotalQty.getOrDefault(date, 0) * 0.8);

            responseList.add(DemandForecastResponse.builder()
                    .storeId(storeId)
                    .forecastDate(date)
                    .predictedSalesMax(totalSales)
                    .predictedVisitors(estimatedVisitors)
                    .build());
        }
        return responseList;
    }
}