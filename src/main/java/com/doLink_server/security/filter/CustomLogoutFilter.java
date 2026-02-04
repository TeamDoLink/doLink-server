package com.doLink_server.security.filter;

import com.doLink_server.auth.service.JwtIssueService;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * OAuth 로그아웃 필터
 */
@Slf4j
@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilterBean {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final JwtIssueService jwtIssueService;
    private final RedisService redisService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        handleLogout((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    /**
     * 로그아웃 요청 처리 메서드
     * <p>
     * 요청 URI와 HTTP 메서드를 확인하여 로그아웃 API 요청인지 판단합니다.
     * 로그아웃 요청인 경우, 쿠키에서 refresh 토큰을 추출하고 유효성 검증을 수행합니다.
     * Redis에 저장된 토큰과 일치하는지 확인 후 로그아웃 처리(토큰 삭제, 쿠키 만료 설정)를 진행합니다.
     * 로그아웃이 아닌 요청은 다음 필터로 정상적으로 전달합니다.
     *
     * @param request     HTTP 요청 객체 (토큰 쿠키 추출 및 요청 정보 확인용)
     * @param response    HTTP 응답 객체 (로그아웃 결과 전송용)
     * @param filterChain 다음 필터 또는 리소스로 요청 전달용 체인
     * @throws IOException      입출력 예외 발생 시 던짐
     * @throws ServletException 서블릿 처리 중 예외 발생 시 던짐
     */
    public void handleLogout(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // 1. 로그아웃 요청이 아닌 경우, 다음 필터로 전달
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request Method: {}", request.getMethod());

        if (!"/v1/user/logout".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 쿠키에서 refresh 토큰 추출
        try {
            Cookie[] cookies = request.getCookies();
            String refreshToken = jwtIssueService.getRefreshTokenFromCookie(cookies);

            // Redis에 저장된 refresh 토큰 확인
            String userId = jwtProvider.getUserId(refreshToken);
            if (redisService.isInvalidRefreshToken(userId, refreshToken)) {
                log.error("Redis에 저장된 RefreshToken과 불일치");
                throw new GeneralException(ErrorStatus.TOKEN_INVALIDATE_ERROR);
            }
            // Redis 토큰 삭제 (로그아웃)
            redisService.removeRefreshToken(userId);

        } catch (ExpiredJwtException e) {
            ApiResponse.sendErrorResponse(response, ErrorStatus.TOKEN_EXPIRED.getReasonHttpStatus(), objectMapper);
            return;
        } catch (GeneralException e) {
            ApiResponse.sendErrorResponse(response, e.getErrorReasonHttpStatus(), objectMapper);
            return;
        }

        // 3. 쿠키 삭제
        Cookie initCookie = jwtIssueService.removeRefreshTokenFromCookie();
        response.addCookie(initCookie);

        // 성공 응답
        ApiResponse.sendSuccessResponse(response, "로그아웃이 완료되었습니다.", objectMapper);
    }

}
