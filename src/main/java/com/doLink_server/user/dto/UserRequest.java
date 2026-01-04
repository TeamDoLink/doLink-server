package com.doLink_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 사용자 프로필 정보 요청 DTO
 */
@Getter
public class UserRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotBlank(message = "프로필 이미지 아이디는 필수입니다.")
    private String profileImageUrl;

    @NotNull(message = "푸시알람 여부는 필수입니다.")
    private boolean pushNotificationEnabled;
}
