package com.erp.erp_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OwnerSettingRequest {
    // ownerId는 인증 토큰에서 추출
    
    @NotBlank
    @Size(max = 50)
    private String settingKey; 

    @Size(max = 255)
    private String settingValue; 
}