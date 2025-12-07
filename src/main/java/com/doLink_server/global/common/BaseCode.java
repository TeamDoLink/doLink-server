package com.doLink_server.global.common;

/**
 * 성공/실패 코드를 통합 관리하기 위한 상위 인터페이스
 */
public interface BaseCode {

    ReasonDTO getReason();
    ReasonDTO getReasonHttpStatus();

}
