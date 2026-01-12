package com.erp.erp_back.dto.ai;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class SalesSummaryDto {
    private String date;        // "2025-01-01"
    private Long storeId;
    private Long menuId;

    // 판매 데이터 (수량, 매출액)
    private Long quantity;      // 판매 수량
    private BigDecimal totalAmount; // 총 매출액

    // 메뉴 정보 (학습용)
    private String menuName;
    private String category;

    // 매장 위치 (날씨 매핑용)
    private int nx;
    private int ny;
}