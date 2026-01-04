package com.doLink_server.auth.service;


import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.security.jwt.JwtProperties;
import com.doLink_server.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 관련 서비스
 */
@Slf4j
@Service
@AllArgsConstructor
public class JwtIssueService {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    /**
     * AccessToken 발급
     * @param userId 사용자 아이디
     * @return 액세스 토큰
     */
    public String issueAccessToken(String userId) {
        return jwtProvider.createJwt(userId, jwtProperties.getAccessTokenExpiration(), "access");
    }

    /**
     * AccessToken 재발급
     * @param refreshToken 리프레시 토큰
     * @return 액세스 토큰
     */
    public String reIssueAccessToken(String refreshToken) {
        String userId = jwtProvider.getUserId(refreshToken);
        validateToken(refreshToken, "refresh");

        return jwtProvider.createJwt(userId, jwtProperties.getAccessTokenExpiration(), "access");
    }

    /**
     * RefreshToken 발급
     * @param userId 사용자 아이디
     * @return 액세스 토큰
     */
    public String issueRefreshToken(String userId) {
        long expiration = jwtProperties.getRefreshTokenExpiration();
        return jwtProvider.createJwt(userId, expiration, "refresh");
    }

    /**
     * 쿠키에서 RefreshToken 추출
     * @param cookies HttpServletRequest
     * @return 리프레시 토큰
     */
    public String getRefreshTokenFromCookie(Cookie[] cookies) {
        String refreshToken = null;
        if (cookies == null) {
            throw new GeneralException(ErrorStatus.TOKEN_INVALIDATE_ERROR);
        }

        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }
        if (refreshToken == null) {
            throw new GeneralException(ErrorStatus.ALREADY_LOGOUT);
        }
        return refreshToken;
    }

    /**
     * 쿠키에서 RefreshToken 삭제
     *
     * @return 쿠키 객체
     */
    public Cookie removeRefreshTokenFromCookie() {
        Cookie deleteCookie = new Cookie("refresh", null);
        deleteCookie.setMaxAge(0);
        deleteCookie.setPath("/");
        deleteCookie.setHttpOnly(true);
        deleteCookie.setSecure(true);

        return deleteCookie;
    }

    /**
     * 전달받은 Token의 유효성을 검증합니다.
     * @param tokenType 클라이언트가 전달한 Refresh Token
     * @throws GeneralException TOKEN_NOT_FOUND: 토큰이 null인 경우
     * @throws GeneralException INVALID_TOKEN: JWT 자체가 유효하지 않거나, Redis에 저장된 토큰과 일치하지 않을 경우
     */
    public void validateToken(String token, String tokenType) {
        if (token == null) {
            throw new GeneralException(ErrorStatus.TOKEN_NOT_FOUND);
        }
        if (!jwtProvider.validateToken(token)) {
            throw new GeneralException(ErrorStatus.TOKEN_SIGNATURE_ERROR);
        }
        if (jwtProvider.isExpired(token)) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        }

        String category = jwtProvider.getCategory(token);
        if (!tokenType.equals(category)) {
            throw new GeneralException(ErrorStatus.TOKEN_SIGNATURE_ERROR);
        }
    }
}
