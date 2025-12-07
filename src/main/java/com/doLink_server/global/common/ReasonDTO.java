package com.doLink_server.global.common;

import lombok.Builder;
import org.springframework.http.HttpStatus;

/**
 * 응답 메시지에 포함될 정보를 담는 불변 클래스
 * - 구조화된 응답 상태 정보 조립용 내부 DTO
 * - 생성자, getter, equals, hashCode, toString 등이 자동 생성
 * - 목적: 데이터를 담기 위한 용도로만 사용되는 객체에 적합 (DTO, VO 등)
 */
@Builder
public record ReasonDTO(
    HttpStatus httpStatus,      //HTTP 상태코드
    boolean isSuccess,          // 성공여부
    String code,                // 비즈니스 코드
    String message              // 사용자에게 보여줄 메시지
) { }
