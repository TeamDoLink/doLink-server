package com.doLink_server.security.filter;


import com.doLink_server.auth.oauth.model.CustomOAuth2UserDetails;
import com.doLink_server.auth.service.JwtIssueService;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.security.dto.UserDTO;
import com.doLink_server.security.exception.CustomAuthenticationException;
import com.doLink_server.security.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 인증을 위한 필터
 */
@Slf4j
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final JwtIssueService jwtIssueService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // jwt 기간 만료시, 무한 재로그인 방지 로직
        String requestUri = request.getRequestURI();
        if (requestUri.matches("^/login(?:/.*)?$") || requestUri.matches("^/oauth2(?:/.*)?$") ||
                requestUri.matches("^/v1/auth/reissue$")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Access Token 추출 (header: Authorization)
        String accessToken = request.getHeader("Authorization");
        log.info("[JwtFilter] accessToken from header: {}", accessToken);
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }
        // "Bearer " 접두어 제거
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        try {
            // ✅ [1] Access Token 유효성 검증 (유효하지 않으면 예외 던짐)
            jwtIssueService.validateToken(accessToken, "access");

            // ✅ [2] 사용자 정보 추출
            String userId = jwtProvider.getUserId(accessToken);

            // ✅ [3] 사용자 정보 객체 설정 및 SecurityContext에 인증 객체 저장
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);
            CustomOAuth2UserDetails customUser = new CustomOAuth2UserDetails(userDTO);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customUser, null, customUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (CustomAuthenticationException ex) {
            // ✅ [4] AuthenticationException 으로 래핑해서 EntryPoint로 위임
            SecurityContextHolder.clearContext(); // 중요: 인증 실패 시 Context 초기화
            throw ex; // 🔥 여기서 EntryPoint로 위임되도록 던짐!
        }

        // 7. 필터 체인 계속 진행
        filterChain.doFilter(request, response);
    }
}