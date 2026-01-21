package com.erp.erp_back.dto.erp;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriceCompetitivenessResponse {

    private Long storeId;
    private Integer radiusM;

    private Integer nearStoreTotalCount; // 이웃 총 수(매출/메뉴유무 무관)

    private TradeAreaInfo tradeArea;     // 이미 만든 DTO 재사용

    private List<PriceCompetitivenessItem> items;
}
