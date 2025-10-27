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
public class OwnerSignUpRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50)
    private String username; 

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Size(max = 100)
    private String email; 

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password; 
}