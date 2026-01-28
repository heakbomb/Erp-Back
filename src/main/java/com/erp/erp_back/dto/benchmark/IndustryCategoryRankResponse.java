package com.erp.erp_back.dto.benchmark;

import java.math.BigDecimal;

public record IndustryCategoryRankResponse(
    String industry,        // CHICKEN, KOREAN ...
    String categoryName,    // menu_item.category_name (중분류)
    long quantity,          // 기간 합계 판매수량
    BigDecimal shareQty,    // 업종 내 점유율(%)
    int rank,               // 1..N
    int sampleCount         // 집계에 포함된 매장 수 (익명성 기준)
) {}