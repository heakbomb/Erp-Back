package com.erp.erp_back.dto.benchmark;

import java.math.BigDecimal;

public record IndustrySubCategoryRankResponse(
    String industry,          // CHICKEN, KOREAN ...
    String categoryName,      // 중분류 (menu_item.category_name)
    String subCategoryName,   // 소분류 (menu_item.sub_category_name)
    long quantity,            // 기간 합계 판매수량
    BigDecimal shareQty,      // 중분류 내 점유율(%)
    int rank,                 // 1..N
    int sampleCount           // 집계에 포함된 매장 수
) {}
