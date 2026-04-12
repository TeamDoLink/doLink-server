package com.doLink_server.auth.controller;

import com.doLink_server.auth.dto.GoogleNativeLoginRequest;
import com.doLink_server.auth.dto.GoogleNativeTokenResult;
import com.doLink_server.auth.service.AuthRefreshCookieWriter;
import com.doLink_server.auth.service.GoogleNativeAuthService;
import com.doLink_server.auth.service.JwtIssueService;
import com.doLink_server.auth.service.RedisService;
import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.security.exception.CustomAuthenticationException;
import com.doLink_server.security.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final JwtIssueService jwtIssueService;
    private final RedisService redisService;
    private final GoogleNativeAuthService googleNativeAuthService;
    private final AuthRefreshCookieWriter authRefreshCookieWriter;

    /**
     * Android/iOS 네이티브 Google Sign-In: ID 토큰 검증 후 JWT 발급
     */
    @PostMapping("/oauth/google/native")
    @Operation(summary = "Google 네이티브 로그인", description = "Google ID 토큰을 검증하고 access·refresh JWT를 발급합니다.")
    public ResponseEntity<ApiResponse<GoogleNativeTokenResult>> googleNativeLogin(
            @Valid @RequestBody GoogleNativeLoginRequest request,
            HttpServletResponse httpResponse
    ) {
        GoogleNativeTokenResult result = googleNativeAuthService.loginWithIdToken(request.idToken());
        authRefreshCookieWriter.addRefreshTokenCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + result.accessToken())
                .body(ApiResponse.onSuccess(result));
    }

    /**
     * AccessToken 재발급
     * @param request http only Cookie 리프레시 토큰 객체
     * @return 액세스 토큰
     */
    @PostMapping("/reissue")
    @Operation(summary = "AccessToken 재발급 요청", description = "만료된 AccessToken 재발급을 요청한다.")
    public ResponseEntity<ApiResponse<String>> issueAccessToken(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        // ✅ Refresh Token 유효성 및 Redis 토큰 일치 확인
        Cookie[] cookies = request.getCookies();
        String refreshToken = jwtIssueService.getRefreshTokenFromCookie(cookies);
        jwtIssueService.validateToken(refreshToken, "refresh");

        String userId = jwtProvider.getUserId(refreshToken);
        if (redisService.isInvalidRefreshToken(userId, refreshToken)) {
            log.error("Redis에 저장된 RefreshToken과 불일치");
            throw new CustomAuthenticationException(ErrorStatus.TOKEN_INVALIDATE_ERROR);
        }

        String newAccessToken = jwtIssueService.reIssueAccessToken(refreshToken);
        ApiResponse<String> response = ApiResponse.onSuccess(newAccessToken);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + newAccessToken)
                .body(response);
    }



}
