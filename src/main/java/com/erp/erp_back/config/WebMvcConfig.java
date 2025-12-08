package com.erp.erp_back.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActiveStoreInterceptor activeStoreInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeStoreInterceptor)
                // ✅ 전체 API에 적용 (프로젝트에서 /api 프리픽스 쓰면 "/api/**"로 바꿔)
                .addPathPatterns("/**")
                // ✅ 필요하면 여기서 추가로 예외 경로 빼기
                .excludePathPatterns(
                        "/error",
                        "/store/**",            // 사업장 관리
                        "/phone-verify/**",     // 전화번호 인증
                        "/business-number/**",  // 사업자번호 인증
                        "/auth/**"              // 로그인/회원가입 계열
                );
    }
}