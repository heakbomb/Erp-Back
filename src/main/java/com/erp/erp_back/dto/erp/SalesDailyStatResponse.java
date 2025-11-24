// src/main/java/com/erp/erp_back/dto/erp/SalesDailyStatResponse.java
package com.erp.erp_back.dto.erp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SalesDailyStatResponse {

    private String date;

    private BigDecimal sales;
}
