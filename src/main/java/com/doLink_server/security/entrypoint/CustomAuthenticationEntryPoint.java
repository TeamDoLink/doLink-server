package com.doLink_server.security.entrypoint;

import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.security.exception.CustomAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출되는 EntryPoint
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorStatus status;

        // CustomAuthenticationException 이라면 우리가 설정한 ErrorStatus 사용
        if (authException instanceof CustomAuthenticationException customEx) {
            status = customEx.getErrorStatus();
        } else {
            // 그 외 기본 인증 실패
            status = ErrorStatus._UNAUTHORIZED;
        }

        ApiResponse<?> apiResponse = ApiResponse.onFailure(status.getCode(), status.getMessage());

        response.setStatus(status.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}