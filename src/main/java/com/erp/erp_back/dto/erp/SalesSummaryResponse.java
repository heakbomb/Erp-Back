package com.erp.erp_back.dto.erp;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse {

    // 오늘 매출 & 전일 대비 증감률(%)
    private BigDecimal todaySales;
    private BigDecimal todaySalesChangeRate;

    // 이번 주 매출 & 전주 대비 증감률(%)
    private BigDecimal weekSales;
    private BigDecimal weekSalesChangeRate;

    // 이번 달 매출 & 전월 대비 증감률(%)
    private BigDecimal monthSales;
    private BigDecimal monthSalesChangeRate;

    // 오늘 기준 평균 객단가 & 전일 대비 증감률(%)
    private BigDecimal avgTicket;
    private BigDecimal avgTicketChangeRate;
}
