package com.doLink_server.auth.oauth.model;

/**
 * 소셜 제공자의 사용자 정보를 공통화하기 위한 인터페이스
 * 각 제공자별 구현체(GoogleUserInfo, KakaoUserInfo 등)가 이걸 구현함
 */
public interface OAuth2UserInfo {
    //제공자 (Ex. naver, google, ...)
    String getProvider();
    //제공자에서 발급해주는 아이디(번호)
    String getProviderId();
    //이메일
    String getEmail();
    //사용자 실명 (설정한 이름)
    String getNickname();
    //사용자 프로필 사진
    String getProfileImageUrl();
}
