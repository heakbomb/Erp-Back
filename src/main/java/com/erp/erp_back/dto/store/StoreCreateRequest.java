package com.erp.erp_back.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreCreateRequest {

    @NotNull(message = "사업자 정보 ID는 필수입니다.")
    private Long bizId;

    @NotBlank(message = "사업장 이름은 필수입니다.")
    @Size(max = 100)
    private String storeName;

    @NotBlank(message = "업종은 필수입니다.")
    @Size(max = 50)
    private String industry;

    @Size(max = 50)
    private String posVendor;

    private Double latitude;
    
    private Double longitude;
}