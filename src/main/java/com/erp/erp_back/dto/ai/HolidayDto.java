package com.erp.erp_back.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HolidayDto {
    private String date;       // "2025-01-01"
    private String name;       // "신정"
    private boolean isHoliday; // true
}