package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriceCompetitivenessItem {

    private Long menuId;
    private String menuName;

    private BigDecimal myPrice;

    private BigDecimal neighborAvgPrice;   // 주변 평균가(동일 메뉴명)
    private Integer neighborStoreCount;    // 평균 산출에 포함된 주변 매장 수

    private BigDecimal diffPrice;          // myPrice - neighborAvgPrice
    private BigDecimal diffRatePct;        // (myPrice - avg)/avg * 100 (%)

    private String verdict;                // CHEAP / FAIR / EXPENSIVE / NO_DATA
}
