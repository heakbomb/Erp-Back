package com.erp.erp_back.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource; // ✅ 변경
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { // ✅ 리턴 타입 및 메소드명 변경
        CorsConfiguration config = new CorsConfiguration();

        // ngrok 및 로컬 환경 허용
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://192.168.0.151:3000",
            "https://*.ngrok-free.app",
            "https://*.ngrok-free.dev"
        ));
        
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS")); // OPTIONS 필수
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source; // ✅ Source 객체 반환
    }
}