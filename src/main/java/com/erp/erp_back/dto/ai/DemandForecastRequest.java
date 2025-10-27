package com.erp.erp_back.dto.ai;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DemandForecastRequest {

    @NotNull
    private Long storeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
