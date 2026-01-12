package com.erp.erp_back.service.ai;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.erp.erp_back.dto.ai.AiTrainingRequestDto;
import com.erp.erp_back.dto.ai.EventDto;
import com.erp.erp_back.dto.ai.HolidayDto;
import com.erp.erp_back.dto.ai.SalesSummaryDto;
import com.erp.erp_back.dto.ai.WeatherRawDto;
import com.erp.erp_back.repository.ai.ExternalEventRepository;
import com.erp.erp_back.repository.ai.HolidayRepository;
import com.erp.erp_back.repository.ai.SalesSummaryRepository;
import com.erp.erp_back.repository.ai.WeatherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiDataService {

    private final SalesSummaryRepository salesRepo;
    private final WeatherRepository weatherRepo;
    private final HolidayRepository holidayRepo;
    private final ExternalEventRepository eventRepo; // ✅ 추가됨

    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public void sendTrainingDataToPython() {
        // 1. 기간 설정 (최근 3년)
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusYears(3);

        System.out.println("AI 데이터 추출 시작...");

        // 2. 데이터 수집 (각각의 요인을 독립적으로 수집)
        // A. 매출 (내부 요인: 메뉴, 할인정보 포함)
        List<SalesSummaryDto> salesList = salesRepo.findRichSalesData(startDate, endDate);

        // B. 날씨 (환경 요인 1)
        List<WeatherRawDto> weatherList = weatherRepo.findRawWeather(startDate, endDate);

        // C. 공휴일 (환경 요인 2)
        List<HolidayDto> holidayList = holidayRepo.findHolidays(startDate, endDate);
        
        // D. 외부 이벤트 (환경 요인 3: 월드컵, 야구 등) ✅
        List<EventDto> eventList = eventRepo.findEvents(startDate, endDate);

        // 3. 포장 (All-in-One)
        AiTrainingRequestDto requestDto = AiTrainingRequestDto.builder()
                .salesList(salesList)
                .weatherList(weatherList)
                .holidayList(holidayList)
                .eventList(eventList) // ✅
                .build();

        // 4. 전송
        String pythonUrl = "http://python-server:8000/train";
        try {
            restTemplate.postForObject(pythonUrl, requestDto, String.class);
            System.out.println("🚀 전송 성공! (이벤트 데이터 " + eventList.size() + "건 포함)");
        } catch (Exception e) {
            System.err.println("❌ 전송 실패: " + e.getMessage());
        }
    }
}