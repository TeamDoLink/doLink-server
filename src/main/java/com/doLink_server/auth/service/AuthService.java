package com.doLink_server.auth.service;


import com.doLink_server.auth.oauth.model.CustomOAuth2UserDetails;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * 인증 관련 서비스
 */
@Service
public class AuthService {

    @Value("${social.kakao.admin-key}")
    private String kakaoAdminKey;
    @Value("${social.kakao.authorization-domain}")
    private String kakaoBaseDomain;
    @Value("${social.kakao.api-uri}")
    private String kakaoUri;

    /**
     * 인증된 유저정보 조회
     * @return String 사용자 아이디
     * @throws GeneralException 로그인 정보 없음 또는 유저 없음
     */
    public String getAuthenticatedUserId() {
        return getCurrentUserDetails()
                .map(CustomOAuth2UserDetails::getUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_USER));
    }

    /**
     * 현재 인증된 사용자 정보 전체 반환
     * @return Optional of CustomOAuth2UserDetails
     */
    private Optional<CustomOAuth2UserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOAuth2UserDetails userDetails) {
            return Optional.of(userDetails);
        }

        return Optional.empty();
    }

    /**
     * 소셜 인증 해제
     * @param socialId 소셜 아이디
     * @param socialName 소셜 명
     */
    public void unlinkAuthenticatedUser(String socialId, String socialName){

        switch (socialName.toLowerCase()) {
            case "kakao" -> unlinkKakao(socialId);
            case "naver" -> throw new GeneralException(ErrorStatus._NOT_IMPLEMENTED_SOCIAL);
            case "google" -> throw new GeneralException(ErrorStatus._NOT_IMPLEMENTED_SOCIAL);
            default -> throw new GeneralException(ErrorStatus._UNSUPPORTED_SOCIAL_PLATFORM);
        }
    }

    /**
     * 카카오 인증 해제
     * @param kakaoUserId 소셜 아이디
     */
    private void unlinkKakao(String kakaoUserId) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(kakaoBaseDomain)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoAdminKey)
                    .build();

            webClient.post()
                    .uri(kakaoUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("target_id_type", "user_id")
                            .with("target_id", kakaoUserId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 필요시 timeout 고려

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._KAKAO_UNLINK_FAILED);
        }
    }
}
