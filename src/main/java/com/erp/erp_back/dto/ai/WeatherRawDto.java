package com.erp.erp_back.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherRawDto {
    private String date;       // "2025-01-01" (forecastDate)
    private String time;       // "15:00" (forecastTime)
    private int nx;
    private int ny;
    
    private Double temp;       // 기온
    private Double rainMm;     // 강수량
    private Integer humidity;  // 습도
    // 필요한 경우 sky, pty 코드 추가
}
