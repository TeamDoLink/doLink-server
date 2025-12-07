package com.doLink_server.global.common;

import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.common.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

/**
 * API의 공통 응답 래퍼 클래스
 */
@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess; // 성공 여부
    private final String code;  // 응답코드
    private final String message;   // 응답 메시지

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result; //실제 응답 데이터 (성공 시 포함)

    // 성공한 경우 응답 생성
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, SuccessStatus._OK.getCode(),
                SuccessStatus._OK.getMessage(), result);
    }

    public static <T> ApiResponse<T> of(BaseCode code, T result) {
        return new ApiResponse<>(true, code.getReasonHttpStatus().code(),
                code.getReasonHttpStatus().message(), result);
    }

    // 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }

    // 데이터 없이 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    // 기본 실패 응답 생성
    public static <T> ApiResponse<T> onFailure(T result) {
        return new ApiResponse<>(false, ErrorStatus._BAD_REQUEST.getCode(),
                ErrorStatus._BAD_REQUEST.getMessage(), result);
    }

    // 게시된 경우 응답 생성
    public static <T> ApiResponse<T> created(T result) {
        return new ApiResponse<>(true, SuccessStatus._CREATED.getCode(),
                SuccessStatus._CREATED.getMessage(), result);
    }

    // 삭제된 경우 응답 생성
    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, SuccessStatus._NO_CONTENT.getCode(),
                SuccessStatus._NO_CONTENT.getMessage(), null);
    }

    public static void sendErrorResponse(HttpServletResponse response, ReasonDTO reason, ObjectMapper objectMapper) throws IOException {
        response.setStatus(reason.httpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<String> errorResponse = onFailure(reason.code(), reason.message());
        String responseBody = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(responseBody);
    }

    public static void sendSuccessResponse(HttpServletResponse response, String message, ObjectMapper objectMapper) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String responseBody = objectMapper.writeValueAsString(onSuccess(message));
        response.getWriter().write(responseBody);
    }
}