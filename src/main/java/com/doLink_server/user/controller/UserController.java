package com.doLink_server.user.controller;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.user.dto.UserRequest;
import com.doLink_server.user.dto.UserResponse;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import com.doLink_server.user.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/user")
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserUseCase userUseCase;
    private final UserService userService;
    private final AuthService authService;

    /**
     * 현재 로그인한 사용자 정보
     * @return 로그인 사용자 정보 또는 로그인 요청 메시지
     */
    @GetMapping("/profile")
    @Operation(summary = "사용자 정보", description = "현재 로그인한 사용자의 정보를 반환한다.")
    public ApiResponse<UserResponse> getUser() {

        // 인증객체에서 사용자 아이디 가져오기
        String userId = authService.getAuthenticatedUserId();
        Users loginUser = userService.findExistingUser(userId);
        UserResponse userResponse = UserResponse.fromEntity(loginUser);
        return ApiResponse.onSuccess(userResponse);
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "Security 로그아웃 필터를 실행한다.")
    @PostMapping("/logout")
    public void logout() {}

    /**
     * 서비스 탈퇴
     */
    @Operation(summary = "회원탈퇴", description = "서비스를 탈퇴한다.")
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<String>> withdraw() {
        String userId = authService.getAuthenticatedUserId();
        userUseCase.withdrawUser(userId);

        return ResponseEntity.ok(
                ApiResponse.onSuccess("회원 탈퇴가 완료되었습니다.")
        );
    }


}
