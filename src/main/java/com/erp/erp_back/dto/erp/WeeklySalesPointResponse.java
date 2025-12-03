package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주간 매출 데이터 포인트
 * weekIndex: 1주차, 2주차 ... 그래프/표 라벨용 인덱스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySalesPointResponse {

    private int weekIndex;             // 1, 2, 3, 4, 5 ...
    private BigDecimal mySales;        // 내 매장 매출
    private BigDecimal areaAvgSales;   // 주변 상권 평균 매출 (지금은 placeholder)
}
