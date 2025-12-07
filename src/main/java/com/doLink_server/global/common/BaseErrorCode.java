package com.doLink_server.global.common;

/**
 * 실패 코드만 별도로 추상화한 인터페이스
 */
public interface BaseErrorCode {

    ReasonDTO getReason();

    ReasonDTO getReasonHttpStatus();
}
