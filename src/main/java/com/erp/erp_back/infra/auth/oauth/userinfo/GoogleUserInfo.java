package com.erp.erp_back.infra.auth.oauth.userinfo;

import java.util.Map;


public class GoogleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        // google: "sub"
        Object sub = attributes.get("sub");
        return sub == null ? null : String.valueOf(sub);
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email == null ? null : String.valueOf(email);
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        if (name != null) return String.valueOf(name);

        // fallback
        Object given = attributes.get("given_name");
        return given == null ? null : String.valueOf(given);
    }

    @Override
    public String getPhone() {
        // 구글 기본 scope에는 phone 없음(추후 People API/추가 scope로 확장)
        return null;
    }
}