package com.erp.erp_back.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MenuItemRequest {

    @NotNull
    private Long storeId; 

    @NotBlank
    @Size(max = 100)
    private String menuName;

    @NotNull
    @Positive
    private BigDecimal price;
}