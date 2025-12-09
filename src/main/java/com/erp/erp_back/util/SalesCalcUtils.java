// com.erp.erp_back.util.SalesCalcUtils.java
package com.erp.erp_back.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesCalcUtils {

    private SalesCalcUtils() {}

    public static BigDecimal calcAvgTicket(BigDecimal totalAmount, long count) {
        if (totalAmount == null || count <= 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount
                .divide(BigDecimal.valueOf(count), 0, RoundingMode.DOWN); // 정책에 맞게 자리/모드 조정
    }
}
