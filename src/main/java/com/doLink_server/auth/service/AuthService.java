package com.doLink_server.auth.service;


import com.doLink_server.auth.oauth.model.CustomOAuth2UserDetails;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

/**
 * 인증 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;

    @Value("${social.kakao.admin-key}")
    private String kakaoAdminKey;
    @Value("${social.kakao.authorization-domain}")
    private String kakaoBaseDomain;
    @Value("${social.kakao.api-uri}")
    private String kakaoUri;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

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
     * @param userId 사용자 아이디 (구글 refresh token 조회용)
     */
    public void unlinkAuthenticatedUser(String socialId, String socialName, String userId){

        switch (socialName.toLowerCase()) {
            case "kakao" -> unlinkKakao(socialId);
            case "google" -> unlinkGoogle(userId);
            case "naver" -> throw new GeneralException(ErrorStatus._NOT_IMPLEMENTED_SOCIAL);
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
                    .block();

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._KAKAO_UNLINK_FAILED);
        }
    }

    /**
     * 구글 인증 해제
     * Redis에 저장된 refresh token으로 access token을 재발급받아 revoke 처리
     * @param userId 사용자 아이디
     */
    private void unlinkGoogle(String userId) {
        String refreshToken = redisService.getSocialRefreshToken(userId);
        if (refreshToken == null) {
            log.warn("▶ 구글 refresh token 없음. UserId: {}", userId);
            return;
        }

        try {
            // refresh token으로 access token 재발급
            String accessToken = reissueGoogleAccessToken(refreshToken);

            // access token으로 revoke
            WebClient.create("https://oauth2.googleapis.com")
                    .post()
                    .uri(uriBuilder -> uriBuilder.path("/revoke").queryParam("token", accessToken).build())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            redisService.removeSocialRefreshToken(userId);
            log.info("▶ 구글 인증 해제 완료. UserId: {}", userId);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._GOOGLE_UNLINK_FAILED);
        }
    }

    /**
     * 구글 refresh token으로 access token 재발급
     */
    private String reissueGoogleAccessToken(String refreshToken) {
        Map response = WebClient.create("https://oauth2.googleapis.com")
                .post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken)
                        .with("client_id", googleClientId)
                        .with("client_secret", googleClientSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }
}
