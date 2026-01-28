package com.doLink_server.domain.task.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.task.dto.LinkCreateResult;
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
    private final LinkCreateService linkCreateService;

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

        // 3) 권한 확인
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        // 4) 링크가 있으면 OG + 썸네일 처리
        String ogImageKey = null;
        String thumbnailKey = null;

        if (request.link() != null && !request.link().isBlank()) {
            LinkCreateResult linkResult = linkCreateService.create(request.link());

            ogImageKey = linkResult.originalKey();
            thumbnailKey = linkResult.thumbnailKey();
        }

        // 5) Task 저장 (thumbnailKey 포함)
        Task saved = taskRepository.save(
                Task.builder()
                        .user(user)
                        .collection(collection)
                        .title(request.title())
                        .link(request.link())
                        .memo(request.memo())
                        .ogImageKey(ogImageKey)
                        .thumbnailKey(thumbnailKey)
                        .inout(true)
                        .status(false)
                        .build()
        );

        return toResponse(saved);
    }

    /**
     * 모음별 Task 전체 조회
     */
    public List<TaskResponse> listByCollection(Long collectionId) {

        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        return taskRepository.findAllByCollection_CollectionIdOrderByTaskIdDesc(collectionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTask(Long taskId) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Task task = taskRepository.findByIdWithUserAndCollection(taskId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_TASK));

        if (!Arrays.equals(task.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        return toResponse(task);
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
