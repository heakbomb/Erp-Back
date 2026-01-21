package com.erp.erp_back.dto.erp;

import java.util.List;

public record MenuAnalyticsResponse(
        List<MenuPoint> menuPerformance,
        List<CategoryPoint> categoryData
) {
    public record MenuPoint(String name, long sales) {}
    public record CategoryPoint(String name, long value) {}
}