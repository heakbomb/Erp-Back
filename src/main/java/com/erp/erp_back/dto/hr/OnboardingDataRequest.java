package com.erp.erp_back.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingDataRequest {

    @NotNull
    private Long storeId; 

    @NotBlank
    @Size(max = 50)
    private String dataType;
}