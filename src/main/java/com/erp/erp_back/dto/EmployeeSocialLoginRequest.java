package com.erp.erp_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeSocialLoginRequest {

    @NotBlank
    @Size(max = 20)
    private String provider; 

    @NotBlank
    @Size(max = 100)
    private String providerId;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
    @NotBlank
    @Size(max = 50)
    private String name; 

    @NotBlank
    @Size(max = 20)
    private String phone; 
}