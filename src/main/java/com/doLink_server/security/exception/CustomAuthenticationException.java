package com.doLink_server.security.exception;


import com.doLink_server.global.common.status.ErrorStatus;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * Spring Security 인증 실패 예외
 */
@Getter
public class CustomAuthenticationException extends AuthenticationException {

    private final ErrorStatus errorStatus;

    public CustomAuthenticationException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}