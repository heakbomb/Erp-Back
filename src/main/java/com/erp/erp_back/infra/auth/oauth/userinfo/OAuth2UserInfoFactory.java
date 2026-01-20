package com.erp.erp_back.infra.auth.oauth.userinfo;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        if (registrationId == null) throw new IllegalArgumentException("registrationId is null");

        String provider = registrationId.trim().toLowerCase();

        return switch (provider) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        };
    }
}