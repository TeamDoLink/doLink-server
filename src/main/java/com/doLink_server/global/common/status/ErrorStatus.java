package com.doLink_server.global.common.status;


import com.doLink_server.global.common.BaseErrorCode;
import com.doLink_server.global.common.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 에러 상황별 코드/메시지/HTTP 상태를 정의한 실제 에러코드
 */
@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // 기본 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청하신 리소스를 찾을 수 없습니다."),

    // Users 에러
    _NOT_FOUND_USER(HttpStatus.NOT_FOUND, "USER404", "사용자가 존재하지 않습니다."),
    _LOGIN_USER_INVALID(HttpStatus.BAD_REQUEST, "USER401", "로그인 중 오류가 발생하였습니다."),
    _INVALID_USER(HttpStatus.BAD_REQUEST, "USER401" , "아이디 또는 비밀번호가 틀렸습니다."),
    _FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "FILE501", "프로필 이미지 업로드 중 오류가 발생하였습니다."),
    _FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "FILE502", "파일을 찾을 수 없습니다."),
    _FILE_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "FILE503", "파일삭제에 실패하였습니다."),
    _WITHDRAW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER500", "사용자 삭제에 실패하였습니다."),

    // Social 인증 해제 에러
    _UNSUPPORTED_SOCIAL_PLATFORM(HttpStatus.BAD_REQUEST, "SOCIAL400", "지원하지 않는 소셜 플랫폼입니다."),
    _NOT_IMPLEMENTED_SOCIAL(HttpStatus.NOT_IMPLEMENTED, "SOCIAL501", "해당 소셜 플랫폼의 해제 기능은 아직 구현되지 않았습니다."),
    _KAKAO_UNLINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL502", "카카오 관리자 인증 해제에 실패했습니다."),

    // Task 에러
    _NOT_FOUND_TASK(HttpStatus.NOT_FOUND, "TASK404", "할 일이 존재하지 않습니다."),
    _UNAUTHORIZED_ACCESS_TASK(HttpStatus.FORBIDDEN, "TASK403", "할 일 권한이 없습니다."),
    _INVALID_PRIORITY(HttpStatus.BAD_REQUEST, "TASK400", "잘못된 우선순위입니다."),
    _INVALID_REPEAT_CONFIGURATION(HttpStatus.BAD_REQUEST, "TASK410", "잘못된 반복 설정입니다."),

    // Memo 에러
    _NOT_FOUND_MEMO(HttpStatus.NOT_FOUND, "MEMO404", "메모가 존재하지 않습니다."),

    // Goal 에러
    _NOT_FOUND_GOAL(HttpStatus.NOT_FOUND, "GOAL404", "목표가 존재하지 않습니다."),

    // Security 에러
    NEED_LOGIN(HttpStatus.NOT_FOUND, "SEC1001", "로그인 후 사용해주세요."),
    ALREADY_LOGOUT(HttpStatus.UNAUTHORIZED, "SEC1002", "이미 로그아웃 된 사용자입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "SEC4001", "잘못된 형식의 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SEC4010", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "SEC419", "토큰이 만료되었습니다."),
    TOKEN_SIGNATURE_ERROR(HttpStatus.UNAUTHORIZED, "SEC4012", "토큰 서명이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "SEC4030", "권한이 없습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "SEC4041", "토큰이 존재하지 않습니다."),
    TOKEN_INVALIDATE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SEC6000", "토큰이 유효하지 않습니다."),
    INTERNAL_SECURITY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SEC5000", "인증 처리 중 서버 에러가 발생했습니다."),
    INTERNAL_TOKEN_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SEC5001", "토큰 처리 중 서버 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder().message(message).code(code).isSuccess(false).build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }

}
