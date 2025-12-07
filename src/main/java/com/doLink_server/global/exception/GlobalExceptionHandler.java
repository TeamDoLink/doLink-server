package com.doLink_server.global.exception;


import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.common.ReasonDTO;
import com.doLink_server.global.common.status.ErrorStatus;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 공통 예외 핸들러
 */
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        ReasonDTO reason = ReasonDTO.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .isSuccess(false)
                .code(ErrorStatus._BAD_REQUEST.getCode())
                .message(e.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.onFailure(reason.code(), reason.message());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException() {
        ReasonDTO reason = ReasonDTO.builder()
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .isSuccess(false)
                .code(ErrorStatus._INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorStatus._INTERNAL_SERVER_ERROR.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.onFailure(reason.code(), reason.message());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
        ReasonDTO reason = e.getErrorReasonHttpStatus();

        ApiResponse<Void> response = ApiResponse.onFailure(reason.code(), reason.message());
        return ResponseEntity.status(reason.httpStatus()).body(response);
    }
}
