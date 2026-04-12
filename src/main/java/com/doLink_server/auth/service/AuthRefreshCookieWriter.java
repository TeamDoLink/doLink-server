package com.doLink_server.auth.service;

import com.doLink_server.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * OAuth 웹 로그인 / 네이티브 로그인 공통: refresh 토큰 HttpOnly 쿠키 설정
 */
@Component
@RequiredArgsConstructor
public class AuthRefreshCookieWriter {

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @Value("${auth.cookie.domain}")
    private String cookieDomain;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure());
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge((int) (jwtProperties.getRefreshTokenExpiration() / 1000));
        response.addCookie(cookie);
    }

    private boolean isSecure() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
