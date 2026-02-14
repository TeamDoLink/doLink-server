package com.doLink_server.user.service;

import com.doLink_server.auth.oauth.model.OAuth2UserInfo;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.task.entity.Task;
import com.doLink_server.domain.task.repository.TaskRepository;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.global.enums.Category;
import com.doLink_server.global.util.UUIDToBytesUtil;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final CollectionRepository collectionRepository;
    private final TaskRepository taskRepository;

    public UserService(UsersRepository usersRepository, CollectionRepository collectionRepository, TaskRepository taskRepository) {
        this.usersRepository = usersRepository;
        this.collectionRepository = collectionRepository;
        this.taskRepository = taskRepository;
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

        // 튜토리얼 데이터 생성
        createTutorialData(user);

        return user;
    }

    private void createTutorialData(Users user) {
        // 모음 생성
        Collection tutorialCollection = Collection.builder()
                .user(user)
                .name("두링크(DoLink) 튜토리얼")
                .category(Category.ETC)
                .isTutorial(true)
                .build();
        collectionRepository.save(tutorialCollection);

        // 할 일 생성
        Task tutorialTask = Task.builder()
                .user(user)
                .collection(tutorialCollection)
                .title("두링크(DoLink) 안내서 📚")
                .link("https://www.notion.so/DoLink-30347f96a7fc8039ae52e566e4c26087?v=25547f96a7fc80b5bce8000c0b3385bb&source=copy_link")
                .memo(null)
                .status(false) // 미완료
                .inout(true)
                .isTutorial(true)
                .build();
        taskRepository.save(tutorialTask);
    }

    /**
     * 탈퇴처리
     */
    public void withdraw(String userId){

        Users user = findExistingUser(userId);

        usersRepository.delete(user);

    }


}
