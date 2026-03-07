package com.doLink_server.auth.oauth.handler;


import com.doLink_server.auth.service.JwtIssueService;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.global.util.UUIDToBytesUtil;
import com.doLink_server.security.jwt.JwtProperties;
import com.doLink_server.security.jwt.JwtProvider;
import com.doLink_server.user.entity.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 인증 성공시 처리 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final JwtProperties jwtProperties;
    private final JwtIssueService jwtIssueService;
    private final Environment environment;

    @Value("${auth.redirect.url}")
    private String redirectUrl;

    @Value("${auth.cookie.domain}")
    private String cookieDomain;

    /**
     * 인증 성공시, 리프레시 토큰을 Http Only 쿠키로 반환한다.
     * 클라이언트가 기존에 가지고 있던 토큰을 쿠키에서 직접 읽어 Redis에 저장된 토큰과 비교한다.
     * 유효하지 않거나 만료된 경우 새로 발급하고 Redis에 저장 후 쿠키에 담아 응답한다.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Users user = (Users) oAuth2User.getAttributes().get("user");
        String userId = UUIDToBytesUtil.convertToEntityAttribute(user.getUserId()).toString();

        // 1. 인증 객체에서 SocialId 추출
        String SocialId = extractSocialId(authentication, response);
        if (SocialId == null) {
            log.warn("▶ SocialId를 추출하지 못해 인증 성공 처리 중단");
            return;
        }

//        // 2. 클라이언트가 가진 refreshToken 쿠키에서 읽기
//        String clientRefreshToken = extractRefreshTokenFromCookie(request);
//        if (clientRefreshToken == null) {
//            log.info("▶ 클라이언트 refreshToken 없음");
//        }
//
//        // 3. Redis에 저장된 refreshToken 조회
//        String redisRefreshToken = redisService.getRefreshToken(userId);
//        if (redisRefreshToken == null) {
//            log.info("▶ Redis에 refreshToken 없음");
//        }

        // 4. 토큰 검증 및 발급 로직
        // 기존 토큰이 있는지, 유효한지 따지지 않고 새로 발급합니다.
        String finalRefreshToken = jwtIssueService.issueRefreshToken(userId);

        // 5. Redis 저장 (기존 키가 있으면 알아서 Overwrite 됩니다)
        redisService.saveRefreshToken(userId, finalRefreshToken);
        log.info("▶ [로그인 성공] 새 refreshToken 발급 및 Redis 갱신 완료. UserId: {}", userId);

        // 5. HttpOnly, Secure 옵션 적용한 쿠키에 refreshToken 세팅
        // accessToken은 쿠키에 담지 않고, 필요하다면 이후 API 요청으로 전달받도록 구성
        addRefreshTokenCookie(response, finalRefreshToken);

//        String accessToken = tokenService.issueAccessToken(userId);
//        response.setHeader("Authorization", "Bearer " + accessToken);
//        response.setContentType("application/json; charset=UTF-8");
//        response.setCharacterEncoding("UTF-8");
//
//        // 공통 응답 포맷으로 JSON 반환
//        Map<String, String> tokenInfo = new HashMap<>();
//        tokenInfo.put("accessToken", accessToken);
//
//        // 6. 로그인 성공 응답 (JSON)
//        ApiResponse<Map<String, String>> apiResponse = ApiResponse.onSuccess(tokenInfo);
//        objectMapper.writeValue(response.getWriter(), apiResponse);

        response.sendRedirect(redirectUrl);

        log.info("▶ OAuth2 인증 성공 후 리다이렉트: {}", redirectUrl);
    }


    /**
     * 인증 객체에서 SocialId를 추출하는 메서드
     */
    private String extractSocialId(Authentication authentication, HttpServletResponse response) throws IOException {
        Object principal = authentication.getPrincipal();

        if (principal instanceof DefaultOAuth2User defaultUser) {
            Object idAttr = defaultUser.getAttribute("id");
            if (idAttr != null) {
                return idAttr.toString();
            } else {
                logAndSendError(response, "DefaultOAuth2User에 id 속성이 없습니다.");
            }
        } else {
            logAndSendError(response, "지원하지 않는 principal 타입: " + principal.getClass());
        }
        return null;
    }

    /**
     * 요청 쿠키에서 refreshToken 값을 추출한다.
     * @param request HttpServletRequest
     * @return 쿠키에 "refresh" 이름으로 저장된 토큰 값, 없으면 null 반환
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refresh".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * HttpOnly, Secure 옵션이 적용된 리프레시 토큰 쿠키를 생성하고 응답에 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh", refreshToken);
        cookie.setHttpOnly(true);  // 자바스크립트에서 접근 불가
        cookie.setSecure(isSecure());  // 환경에 따라 true/false
        cookie.setPath("/");       // 모든 경로에 대해 유효
        cookie.setDomain(cookieDomain); // 루트 도메인
        cookie.setMaxAge((int) (jwtProperties.getRefreshTokenExpiration() / 1000));  // 초 단위 만료시간
        response.addCookie(cookie);
    }

    private boolean isSecure() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                return true;
            }
        }
        return false; // local, dev 등의 경우
    }

    /**
     * 오류 로그를 기록하고 401 Unauthorized 응답 전송
     */
    private void logAndSendError(HttpServletResponse response, String message) throws IOException {
        log.error(message);

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /**
     * JWT 토큰의 Claims(payload)를 JSON 형식으로 예쁘게 로그에 출력한다.
     * 디버깅용으로 Redis와 클라이언트 토큰 비교 시 호출하면 유용하다.
     */
    private void logTokenDetails(String label, String token) {
        jwtProvider.extractClaimsAsJson(token).ifPresentOrElse(
                json -> log.info(">>> JWT Claims [{}]:\n{}", label, json),
                () -> log.warn("JWT Claims [{}]: 유효하지 않아 파싱 실패", label)
        );
    }
}