package com.erp.erp_back.dto.erp;

import java.util.List;

public record WeeklyAreaAvgResponse(
    Long storeId,
    int year,
    int month,
    int nearStoreTotalCount,
    List<WeeklyAreaAvgPoint> data
) {} 