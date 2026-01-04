package com.doLink_server.user.dto;

import com.doLink_server.user.entity.Users;

public record UserResponse(
        String socialName,
        String nickname,
        String email,
        String profileImageUrl
) {
    /**
     * 엔티티 → DTO 변환 팩토리 메서드
     * @param user 사용자 객체
     * @return 사용자 응답정보
     */
    public static UserResponse fromEntity(Users user) {
        return new UserResponse(
                user.getSocialName(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }

}
