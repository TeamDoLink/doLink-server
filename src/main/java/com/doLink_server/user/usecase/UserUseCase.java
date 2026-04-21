package com.doLink_server.user.usecase;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUseCase {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 회원 탈퇴 전용 트랜잭션 로직
     */
    @Transactional
    public void withdrawUser(String userId) {
        Users loginUser = userService.findExistingUser(userId);

        try {
            // 1. 소셜 인증 해제
            authService.unlinkAuthenticatedUser(
                    loginUser.getSocialId(),
                    loginUser.getSocialName(),
                    userId
            );

            // 2. 사용자 데이터 삭제 (연관 테이블 포함)
            userService.withdraw(userId);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._WITHDRAW_FAILED);
        }
    }

}
