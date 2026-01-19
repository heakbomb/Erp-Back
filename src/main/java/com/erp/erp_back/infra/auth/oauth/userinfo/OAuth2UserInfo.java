package com.erp.erp_back.infra.auth.oauth.userinfo;

public interface OAuth2UserInfo {
    String getProvider();      // google / kakao
    String getProviderId();    // provider unique id
    String getEmail();         // nullable 가능
    String getName();          // nullable 가능(없으면 대체값)
    String getPhone();         // nullable 가능
}