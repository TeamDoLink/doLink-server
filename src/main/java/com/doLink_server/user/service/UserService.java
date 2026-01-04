package com.doLink_server.user.service;

import com.doLink_server.auth.oauth.model.OAuth2UserInfo;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.global.util.UUIDToBytesUtil;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UsersRepository usersRepository;

    public UserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    /**
     * 기존 사용자 확인
     *
     * @param userInfo OAuth2 소셜 사용자 정보
     * @return 사용자 응답객체
     */
    public Users findExistingUser(OAuth2UserInfo userInfo) {
        return usersRepository.findBySocialId(userInfo.getProviderId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_USER));
    }

    /**
     * 기존 사용자 확인
     *
     * @param userId 사용자 아이디
     * @return 사용자 응답객체
     */
    public Users findExistingUser(String userId) {
        return usersRepository.findByUserId(UUIDToBytesUtil.convertToDatabaseColumn(userId))
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_USER));
    }

    /**
     * 신규 가입 처리
     * @param userInfo OAuth2 소셜 사용자 정보
     * @return 사용자 응답객체
     */
    public Users join(OAuth2UserInfo userInfo) {

        UUID userId = UUID.randomUUID();
        Users user = Users.builder()
                .userId(UUIDToBytesUtil.convertToDatabaseColumn(userId)) // UUID를 BINARY(16)로 저장
                .socialId(userInfo.getProviderId())
                .socialName(userInfo.getProvider())
                .nickname(userInfo.getNickname())
                .email(userInfo.getEmail())
                .isActive(true)
                .profileImageUrl(userInfo.getProfileImageUrl())
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
        usersRepository.save(user);
        return user;
    }

    /**
     * 탈퇴처리
     */
    public void withdraw(String userId){

        Users user = findExistingUser(userId);

        usersRepository.delete(user);

    }


}
