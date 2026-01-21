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
import com.erp.erp_back.dto.ai.MenuGrowthResponse;
import com.erp.erp_back.entity.ai.DemandForecast;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.SalesMenuDailySummary;
import com.erp.erp_back.repository.ai.DemandForecastRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.SalesMenuDailySummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDataService {

    private final DemandForecastRepository demandForecastRepository;
    private final MenuItemRepository menuItemRepository;
    private final SalesMenuDailySummaryRepository salesMenuDailySummaryRepository;
    private final RestClient restClient;

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

    // ✅ 1. 주간 수요 예측 (DB 데이터 합산)
    public List<DemandForecastResponse> getWeeklyForecast(Long storeId) {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(6);

        // DB에서 해당 기간 예측값 조회
        List<DemandForecast> forecasts = demandForecastRepository.findByStoreIdAndTargetDateBetween(storeId, startDate, endDate);
        
        // 가격 계산을 위해 메뉴 정보 조회
        List<Long> menuIds = forecasts.stream().map(DemandForecast::getMenuId).distinct().collect(Collectors.toList());
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuIds);
        Map<Long, BigDecimal> priceMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getMenuId, MenuItem::getPrice));

        // 날짜별 합계 계산
        Map<LocalDate, BigDecimal> dailyTotalSales = new HashMap<>();
        Map<LocalDate, Integer> dailyTotalQty = new HashMap<>();

        for (DemandForecast f : forecasts) {
            LocalDate date = f.getTargetDate();
            BigDecimal price = priceMap.getOrDefault(f.getMenuId(), BigDecimal.ZERO);
            BigDecimal sales = price.multiply(BigDecimal.valueOf(f.getPredictedQty()));

            dailyTotalSales.merge(date, sales, BigDecimal::add);
            dailyTotalQty.merge(date, f.getPredictedQty(), Integer::sum);
        }

        // 결과 리스트 생성
        List<DemandForecastResponse> responseList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal totalSales = dailyTotalSales.getOrDefault(date, BigDecimal.ZERO);
            // 방문객 수는 단순히 판매 수량의 80%로 가정 (추정치)
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

    // ✅ 2. 메뉴 트렌드 분석 (지난주 vs 다음주 예측 비교)
    public List<MenuGrowthResponse> getMenuGrowthAnalysis(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDate lastWeekEnd = today.minusDays(1);
        LocalDate lastWeekStart = lastWeekEnd.minusDays(6);
        LocalDate nextWeekStart = today.plusDays(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);

        // 1. 지난주 판매 데이터 (실제)
        List<SalesMenuDailySummary> pastSales = salesMenuDailySummaryRepository
                .findByStoreIdAndSummaryDateBetween(storeId, lastWeekStart, lastWeekEnd);
        
        Map<Long, Long> pastSalesMap = pastSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMenuItem().getMenuId(),
                        Collectors.summingLong(SalesMenuDailySummary::getTotalQuantity)
                ));

        // 2. 다음주 예측 데이터 (DB)
        List<DemandForecast> forecasts = demandForecastRepository
                .findByStoreIdAndTargetDateBetween(storeId, nextWeekStart, nextWeekEnd);

        Map<Long, Long> forecastMap = forecasts.stream()
                .collect(Collectors.groupingBy(
                        DemandForecast::getMenuId,
                        Collectors.summingLong(df -> (long) df.getPredictedQty())
                ));

        // 3. 매장의 모든 메뉴에 대해 증감률 계산
        List<MenuItem> menuItems = menuItemRepository.findByStoreStoreId(storeId);
        List<MenuGrowthResponse> result = new ArrayList<>();

        for (MenuItem menu : menuItems) {
            Long pastQty = pastSalesMap.getOrDefault(menu.getMenuId(), 0L);
            Long nextQty = forecastMap.getOrDefault(menu.getMenuId(), 0L);

            // 판매도 없었고 예측도 없으면 제외
            if (pastQty == 0 && nextQty == 0) continue;

            double growthRate = 0.0;
            if (pastQty > 0) {
                growthRate = ((double) (nextQty - pastQty) / pastQty) * 100.0;
            } else if (nextQty > 0) {
                growthRate = 100.0; // 0 -> 양수 (신규 급상승)
            }

            // 추천 로직
            String recommendation = "유지";
            if (growthRate >= 20.0) recommendation = "발주 증량";
            else if (growthRate >= 10.0) recommendation = "소폭 증량";
            else if (growthRate <= -20.0) recommendation = "재고 소진 집중";
            else if (growthRate <= -10.0) recommendation = "발주 감소";

            result.add(MenuGrowthResponse.builder()
                    .menuId(menu.getMenuId())
                    .menuName(menu.getMenuName())
                    .lastWeekSales(pastQty)
                    .nextWeekPrediction(nextQty)
                    .growthRate(Math.round(growthRate * 10.0) / 10.0) // 소수점 첫째자리 반올림
                    .recommendation(recommendation)
                    .build());
        }

        // 증감률 높은 순으로 정렬
        result.sort((a, b) -> Double.compare(b.getGrowthRate(), a.getGrowthRate()));
        
        return result;
    }
}