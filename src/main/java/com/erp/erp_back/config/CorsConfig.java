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

    // 프론트가 뜰 수 있는 모든 오리진 허용 (localhost/127.0.0.1/내 로컬 IP)
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://192.168.0.151:3000"
    ));

    // 프리플라이트 포함 모든 메소드/헤더 허용
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));

    // 쿠키/인증정보 쓰는 경우 대비
    config.setAllowCredentials(true);

    // 브라우저가 프리플라이트 결과 캐싱
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(0); // 가장 먼저 적용
    return bean;
  }
}