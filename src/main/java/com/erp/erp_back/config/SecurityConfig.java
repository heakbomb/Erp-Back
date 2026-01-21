package com.erp.erp_back.config;

import com.erp.erp_back.infra.auth.jwt.JwtAuthFilter;
import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.infra.auth.oauth.CustomOAuth2UserService;
import com.erp.erp_back.infra.auth.oauth.EmployeeOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource; // ✅ 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final EmployeeOAuth2SuccessHandler employeeOAuth2SuccessHandler;
    
    // ✅ CorsConfig에서 등록한 빈 주입
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ [핵심] CORS 설정을 시큐리티에 통합 (인증 필터보다 먼저 동작함)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            .csrf(csrf -> csrf.disable())

            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .authorizeHttpRequests(auth -> auth
                // 로그인 없이 접근해야 하는 경로들
                .requestMatchers(
                    "/auth/login/**",
                    "/auth/register/**",
                    "/auth/email-verifications/**",
                    "/auth/token/refresh",
                    "/error",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/employee/social/callback",
                    "/auth/password/reset/request",
                    "/auth/password/reset/confirm"
                ).permitAll()

                // 관리자 로그인 경로 허용
                .requestMatchers("/auth/admin/login").permitAll()
                
                // 사장님용 구독 상품 조회 (GET) 허용
                .requestMatchers(HttpMethod.GET, "/owner/subscriptions/products").permitAll() // 필요 시 authenticated()로 변경 가능하나 CORS 문제 확인 차원에서는 permitAll 추천 혹은 유지

                // 권한별 보호
                .requestMatchers("/owner/**").hasRole("OWNER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")

                .anyRequest().authenticated()
            )

            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(employeeOAuth2SuccessHandler)
            )

            .addFilterBefore(new JwtAuthFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}