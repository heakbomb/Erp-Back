package com.erp.erp_back.dto.hr;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSettingResponse {
    private Long settingId;
    private Long employeeId;
    private Long storeId;
    private String wageType;
    private BigDecimal baseWage;
    private String deductionItems; // (JSON)
}
