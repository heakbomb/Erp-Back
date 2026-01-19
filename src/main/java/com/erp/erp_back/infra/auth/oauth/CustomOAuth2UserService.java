package com.erp.erp_back.infra.auth.oauth;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.erp.erp_back.infra.auth.oauth.userinfo.OAuth2UserInfoFactory;

/**
 * OAuth2 공급자(google/kakao)로부터 받은 user attribute를
 * 우리쪽 표준 형태로 파싱 가능한지 사전 검증.
 *
 * DB 저장/토큰 발급은 SuccessHandler에서 처리.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId =
                userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // ✅ 공급자별 attribute 구조 검증 목적 (미사용 경고 제거)
        OAuth2UserInfoFactory.of(registrationId, attributes);

        return oAuth2User;
    }
}