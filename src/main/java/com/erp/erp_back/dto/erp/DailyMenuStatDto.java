package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

public interface DailyMenuStatDto {
    Long getStoreId();
    Long getMenuId();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
}