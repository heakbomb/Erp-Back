// src/main/java/com/erp/erp_back/config/CorsConfig.java
package com.erp.erp_back.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // [수정] setAllowedOrigins 대신 setAllowedOriginPatterns 사용
    // ngrok 주소는 실행 시마다 바뀌므로, 와일드카드(*) 패턴을 허용하는 것이 편리합니다.
    config.setAllowedOriginPatterns(List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://192.168.0.151:3000",
        //"https://*.ngrok-free.app" // ✅ 모든 ngrok 서브도메인을 허용
        "https://diphthongic-apolonia-aggravatingly.ngrok-free.dev"
    ));
    
    // [기존 설정 유지]
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(0); // 가장 먼저 적용
    return bean;
  }
}
