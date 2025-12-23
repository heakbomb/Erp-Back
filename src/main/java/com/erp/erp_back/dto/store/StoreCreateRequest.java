package com.erp.erp_back.dto.store;

import com.erp.erp_back.entity.enums.StoreIndustry; // Enum Import

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // NotBlank 대신 NotNull 사용
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

    // ✅ [수정] String -> StoreIndustry, @NotBlank -> @NotNull
    @NotNull(message = "업종은 필수입니다.")
    private StoreIndustry industry;

    @Size(max = 50)
    private String posVendor;

    @NotNull(message = "위치 정보(위도)는 필수입니다.")
    private Double latitude;
    
    @NotNull(message = "위치 정보(경도)는 필수입니다.")
    private Double longitude;

    private Integer gpsRadiusM;
}