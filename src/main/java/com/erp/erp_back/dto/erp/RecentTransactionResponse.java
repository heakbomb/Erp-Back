package com.erp.erp_back.dto.erp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionResponse {

    private Long id;        // 거래번호
    private String time;    // "HH:mm" 형식 시간
    private String items;   // "아메리카노 x2, 라떼 x1" 같은 요약
    private BigDecimal amount; // 총 금액
}
