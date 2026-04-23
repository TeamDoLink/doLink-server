package com.doLink_server.auth.oauth.service;


import com.doLink_server.auth.oauth.model.GoogleUserInfo;
import com.doLink_server.auth.oauth.model.KakaoUserInfo;
import com.doLink_server.auth.oauth.model.OAuth2UserInfo;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.global.util.UUIDToBytesUtil;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 로그인 성공 후 추가 처리를 위한 커스텀 구현체
 */
@Service
@Slf4j
@AllArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final RedisService redisService;

    /**
     * OAuth2 로그인 성공 시 호출되는 메서드
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = logAttributes(oAuth2User.getAttributes());
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = extractSocialAttributeName(userRequest);

        OAuth2UserInfo userInfo = createOAuth2UserInfo(registrationId, attributes);
        Users user = findOrRegisterUser(userInfo);

        Map<String, Object> extendedAttributes = buildExtendedAttributes(attributes, user);
        extendedAttributes.put("registrationId", registrationId);
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_USER");

        return new DefaultOAuth2User(authorities, extendedAttributes, userNameAttributeName);
    }

    /**
     * 제공자별 유저 정보 파싱을 위한 DTO 생성
     */
    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "google" -> new GoogleUserInfo(attributes);
            // todo 네이버
            default -> throw new OAuth2AuthenticationException("지원하지 않는 로그인 제공자입니다: " + registrationId);
        };
    }

    /**
     * 사용자 존재 여부를 확인 후 없으면 자동 회원가입
     */
    private Users findOrRegisterUser(OAuth2UserInfo userInfo) {
        try {
            return userService.findExistingUser(userInfo);
        } catch (GeneralException e) {
            // 유저 없어서 예외 발생 시 신규 가입 처리
            return userService.join(userInfo);
        }
    }

    /**
     * 확장된 인증 객체에 사용자 엔티티 추가
     */
    private Map<String, Object> buildExtendedAttributes(Map<String, Object> attributes, Users user) {
        Map<String, Object> extendedAttributes = new HashMap<>(attributes);
        extendedAttributes.put("user", user);
        return extendedAttributes;
    }

    /**
     * 사용자 식별을 위한 플랫폼 소셜명 추출
     */
    private String extractSocialAttributeName(OAuth2UserRequest userRequest) {
        return userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
    }

    /**
     * 디버깅을 위한 사용자 정보 로깅
     */
    private Map<String, Object> logAttributes(Map<String, Object> attributes) {
        attributes.forEach((key, value) -> log.info("{} = {}", key, value));
        return attributes;
    }

}