package com.erp.erp_back.infra.auth.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.infra.auth.jwt.JwtTokenProvider;
import com.erp.erp_back.infra.auth.oauth.userinfo.OAuth2UserInfo;
import com.erp.erp_back.infra.auth.oauth.userinfo.OAuth2UserInfoFactory;
import com.erp.erp_back.service.auth.EmployeeSocialAuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmployeeOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final EmployeeSocialAuthService employeeSocialAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.social-callback:http://localhost:3000/employee/social/callback}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 principal is not OAuth2User");
            return;
        }

        String registrationId = resolveRegistrationId(request);
        if (registrationId == null || registrationId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot resolve OAuth2 provider");
            return;
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, attributes);

        Employee saved = employeeSocialAuthService.upsertEmployee(userInfo);

        String token = jwtTokenProvider.createAccessToken(
                saved.getEmployeeId(),
                saved.getEmail(),
                "EMPLOYEE"
        );

        String accessToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

        // ✅ provider도 같이 넘기면 프론트에서 디버깅/처리 쉬움(선택)
        String redirectUrl = frontendCallbackUrl
                + "?employeeId=" + saved.getEmployeeId()
                + "&provider=" + URLEncoder.encode(registrationId, StandardCharsets.UTF_8)
                + "&accessToken=" + accessToken;

        response.sendRedirect(redirectUrl);
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        String uri = request.getRequestURI(); // /login/oauth2/code/google
        if (uri == null) return null;
        String[] parts = uri.split("/");
        if (parts.length == 0) return null;
        return parts[parts.length - 1];
    }
}