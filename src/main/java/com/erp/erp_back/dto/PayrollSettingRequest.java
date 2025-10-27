package com.erp.erp_back.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PayrollSettingRequest {

    @NotNull
    private Long employeeId; 

    @NotNull
    private Long storeId; 

    @NotBlank
    @Size(max = 20)
    private String wageType; // (예: "MONTHLY", "HOURLY")

    @NotNull
    @PositiveOrZero
    private BigDecimal baseWage;
    private String deductionItems; // JSON 문자열
}