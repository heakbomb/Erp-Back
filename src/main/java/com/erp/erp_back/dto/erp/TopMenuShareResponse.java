package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인기 메뉴 매출 구성 (파이 차트용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMenuShareResponse {

    private String menuName;       // 메뉴명
    private BigDecimal sales;      // 해당 메뉴 매출액
    private BigDecimal rate;       // 전체 매출 대비 비율 (%)
}
