package com.doLink_server.global.exception;


import com.doLink_server.global.common.BaseErrorCode;
import com.doLink_server.global.common.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException{

    private BaseErrorCode code;

    public ReasonDTO getErrorReasonHttpStatus() {
        return this.code.getReasonHttpStatus();
    }

    @Override
    public String getMessage() {
        return code != null ? code.getReason().message() : super.getMessage();
    }

}
