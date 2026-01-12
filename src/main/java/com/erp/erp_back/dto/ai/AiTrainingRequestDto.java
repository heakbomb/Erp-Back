package com.erp.erp_back.dto.ai;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AiTrainingRequestDto {
    // 1. 예측 대상 (Target)
    private List<SalesSummaryDto> salesList; // (아래에서 수정된 버전 확인)

    // 2. 환경 변수들 (Features)
    private List<WeatherRawDto> weatherList; // 기상청 데이터
    private List<HolidayDto> holidayList;    // 공휴일
    
    // ✅ 추가됨: 월드컵, 야구 등 외부 요인
    private List<EventDto> eventList;        
}