package com.erp.erp_back.config;

import com.erp.erp_back.infra.auth.jwt.JwtAuthFilter;
import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.infra.auth.oauth.CustomOAuth2UserService;
import com.erp.erp_back.infra.auth.oauth.EmployeeOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final EmployeeOAuth2SuccessHandler employeeOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            // OAuth2 과정에서만 세션 필요할 수 있음
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/login/**",
                    "/auth/register/**",
                    "/auth/email-verifications/**",
                    "/auth/token/refresh",
                    "/error",

                    // OAuth2 로그인 기본 엔드포인트
                    "/oauth2/**",
                    "/login/oauth2/**",

                    // ✅ 중요: 프론트 콜백(토큰 저장 전 접근해야 함)
                    "/employee/social/callback"
                ).permitAll()

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