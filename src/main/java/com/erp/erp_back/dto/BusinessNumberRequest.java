package com.erp.erp_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BusinessNumberRequest {

    // ownerId는 인증 토큰에서 추출하여 사용
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @Size(max = 20) 
    private String phone; 

    @NotBlank(message = "사업자번호는 필수입니다.")
    @Size(max = 10)
    private String bizNum; 
}