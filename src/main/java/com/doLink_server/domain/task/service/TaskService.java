package com.doLink_server.domain.task.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.task.dto.LinkCreateResult;
import com.doLink_server.domain.task.dto.TaskCreateRequest;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.dto.TaskUpdateRequest;
import com.doLink_server.domain.task.entity.Task;
import com.doLink_server.domain.task.repository.TaskRepository;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.infra.s3.S3PresignedUrlProvider;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final CollectionRepository collectionRepository;
    private final AuthService authService;
    private final UserService userService;
    private final LinkCreateService linkCreateService;
    private final S3PresignedUrlProvider s3PresignedUrlProvider;

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
     * 단일 Task 조회
     */
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

    /**
     * 모음별 Task 전체 조회 (페이징)
     */
    public Slice<TaskResponse> listByCollection(Long collectionId, int page, int size, String sort) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 권한 확인
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // 정렬: Task ID 기준 내림차순 (최신순)
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, "taskId"));

        return taskRepository.findAllByCollection_CollectionId(collectionId, pageable)
                .map(this::toResponse);
    }

    /**
     * 할 일 수정
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Task task = taskRepository.findByIdWithUserAndCollection(taskId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_TASK));

        // 권한 확인
        if (!Arrays.equals(task.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        task.updateElements(request.title(), request.memo(), request.status(), request.inout());

        // 링크 수정 로직
        if (request.link() != null) {
            String newLink = request.link();

            if (newLink.isBlank()) {
                // 링크 삭제
                task.updateLink(null, null, null);
            } else if (!newLink.equals(task.getLink())) {
                // 링크 변경 (새로 파싱)
                LinkCreateResult linkResult = linkCreateService.create(newLink);
                task.updateLink(newLink, linkResult.originalKey(), linkResult.thumbnailKey());
            }
        }

        return toResponse(task);
    }

    /**
     * 할 일 완료 처리
     */
    @Transactional
    public TaskResponse completeTask(Long taskId) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Task task = taskRepository.findByIdWithUserAndCollection(taskId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_TASK));

        // 권한 확인
        if (!Arrays.equals(task.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        // 이미 완료인 경우 그냥 현재 상태 반환
        if (Boolean.TRUE.equals(task.getStatus())) {
            return toResponse(task);
        }

        task.setStatus(true);

        return toResponse(task);
    }

    /**
     * 할 일 삭제
     */
    @Transactional
    public void deleteTask(Long taskId) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Task task = taskRepository.findByIdWithUser(taskId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_TASK));

        if (!Arrays.equals(task.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        taskRepository.delete(task);
    }

    private TaskResponse toResponse(Task t) {
        String thumbnailUrl = null;
        if (t.getThumbnailKey() != null && !t.getThumbnailKey().isBlank()) {
            thumbnailUrl = s3PresignedUrlProvider.presignGetUrl(t.getThumbnailKey());
        }

        return TaskResponse.builder()
                .taskId(t.getTaskId())
                .collectionId(t.getCollection().getCollectionId())
                .title(t.getTitle())
                .link(t.getLink())
                .memo(t.getMemo())
                .thumbnailUrl(thumbnailUrl)
                .status(t.getStatus())
                .inout(t.getInout())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
