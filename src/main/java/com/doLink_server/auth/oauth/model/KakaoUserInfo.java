package com.doLink_server.auth.oauth.model;

import java.util.Map;

/**
 * 카카오 사용자 정보 DTO
 */
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        // attributes.get("id")는 Long 또는 Integer일 수 있으므로 toString()
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = getMap(attributes, "kakao_account");
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getNickname() {
        Map<String, Object> profile = getProfile();
        return profile != null ? (String) profile.get("nickname") : null;
    }

    public String getProfileImageUrl() {
        Map<String, Object> profile = getProfile();
        return profile != null ? (String) profile.get("profile_image_url") : null;
    }

    private Map<String, Object> getProfile() {
        Map<String, Object> kakaoAccount = getMap(attributes, "kakao_account");
        return kakaoAccount != null ? getMap(kakaoAccount, "profile") : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

}