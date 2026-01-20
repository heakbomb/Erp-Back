package com.erp.erp_back.infra.auth.oauth.userinfo;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = safeMap(attributes.get("kakao_account"));
        this.profile = kakaoAccount == null ? null : safeMap(kakaoAccount.get("profile"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public String getEmail() {
        // 이메일은 안 써도 됨 (scope 제거했다면 null이 정상)
        if (kakaoAccount == null) return null;
        Object email = kakaoAccount.get("email");
        return email == null ? null : String.valueOf(email);
    }

    @Override
    public String getName() {
        // nickname이 없으면 providerId 기반으로 fallback (DB NOT NULL 방지)
        String nickname = null;
        if (profile != null) {
            Object v = profile.get("nickname");
            if (v != null) nickname = String.valueOf(v).trim();
        }
        if (nickname != null && !nickname.isBlank()) return nickname;

        String pid = getProviderId();
        return (pid == null || pid.isBlank()) ? "kakao_user" : "kakao_" + pid;
    }

    @Override
    public String getPhone() {
        // phone_number는 동의/권한에 따라 없을 수 있음
        if (kakaoAccount == null) return null;
        Object phone = kakaoAccount.get("phone_number");
        return phone == null ? null : String.valueOf(phone);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }
}