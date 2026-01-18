package com.doLink_server.domain.task.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.task.dto.TaskCreateRequest;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.entity.Task;
import com.doLink_server.domain.task.repository.TaskRepository;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final CollectionRepository collectionRepository;
    private final AuthService authService;
    private final UserService userService;

    /**
     * 할 일 추가
     */
    @Transactional
    public TaskResponse taskCreate(TaskCreateRequest request) {

        // 1) 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2) 모음 조회
        Collection collection = collectionRepository.findByIdWithUser(request.collectionId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 3) 권한 확인 (내 모음인지) (403)
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }
        
        // 4) Task 저장
        Task saved = taskRepository.save(
                Task.builder()
                        .user(user)
                        .collection(collection)
                        .title(request.title())
                        .link(request.link())
                        .memo(request.memo())
                        .inout(true) // 내부추가는 true
                        .status(false) // 할 일 false 시작
                        .build()
        );

        // 4) 응답
        return toResponse(saved);
    }

    /**
     * 모음별 Task 전체 조회
     */
    public List<TaskResponse> listByCollection(Long collectionId) {

        // 1) 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2) 모음 조회 (없으면 404)
        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 3) 권한 확인 (내 모음인지) (403)
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        // 4) 해당 모음의 Task 전체 조회
        return taskRepository.findAllByCollection_CollectionIdOrderByTaskIdDesc(collectionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.builder()
                .taskId(t.getTaskId())
                .collectionId(t.getCollection().getCollectionId())
                .title(t.getTitle())
                .link(t.getLink())
                .memo(t.getMemo())
                .status(t.getStatus())
                .inout(t.getInout())
                .build();
    }
}
