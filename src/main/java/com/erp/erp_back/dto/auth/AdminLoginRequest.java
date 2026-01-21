package com.erp.erp_back.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminLoginRequest {

    // ✅ @Email 제거: "admin" 같은 일반 아이디도 허용
    @NotBlank(message = "아이디는 필수입니다.")
    private String email; // 프론트엔드 JSON 키("email")와 맞추기 위해 변수명 유지

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}