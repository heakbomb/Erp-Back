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
public class TopMenuStatsResponse {

    private long menuId;
    private String menuName;     
    private long quantity;     
    private BigDecimal revenue; // 매출액(금액 합계)
    
}
