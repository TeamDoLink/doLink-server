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
                        .inout(request.inout())
                        .status(false)
                        .isTutorial(false)
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
    public Slice<TaskResponse> listByCollection(Long collectionId, int page, int size, String sort, Boolean completed) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 권한 확인
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // 정렬: Task ID 기준
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, "taskId"));

        if (completed == null) {
            return taskRepository.findAllByCollection_CollectionId(collectionId, pageable)
                    .map(this::toResponse);
        }

        // completed=true -> status=true(완료), completed=false -> status=false(미완료)
        return taskRepository.findAllByCollection_CollectionIdAndStatus(collectionId, completed, pageable)
                .map(this::toResponse);
    }

    /**
     * 사용자별 전체 할 일 조회 (페이징)
     * - 모든 모음의 할 일을 최신순으로 조회
     */
    public Slice<TaskResponse> listAll(int page, int size) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        PageRequest pageable = PageRequest.of(page, size);

        return taskRepository.findAllByUser(user, pageable)
                .map(this::toResponse);
    }

    /**
     * 사용자별 최근 할 일 조회 (limit 개수 제한)
     * - 최신순으로 limit 개수만큼 조회
     */
    public List<TaskResponse> listRecent(int limit) {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        return taskRepository.findRecentTasksByUser(user, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
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

        // 튜토리얼은 수정 불가능
        if (Boolean.TRUE.equals(task.getIsTutorial())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        // 모음 변경 로직
        if (request.collectionId() != null && !request.collectionId().equals(task.getCollection().getCollectionId())) {
            Collection newCollection = collectionRepository.findByIdWithUser(request.collectionId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

            // 새로운 모음도 내 모음인지 확인
            if (!Arrays.equals(newCollection.getUser().getUserId(), user.getUserId())) {
                throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
            }

            task.setCollection(newCollection);
        }

        task.updateElements(request.title(), request.memo(), request.inout());

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
     * 할 일 완료 상태 토글
     * - 완료(true) -> 미완료(false)
     * - 미완료(false) -> 완료(true)
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

        // 상태 토글
        task.toggleStatus();

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

        // 튜토리얼은 삭제 불가능
        if (Boolean.TRUE.equals(task.getIsTutorial())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_TASK);
        }

        taskRepository.delete(task);
    }

    private TaskResponse toResponse(Task t) {
        String thumbnailUrl = null;
        if (t.getThumbnailKey() != null && !t.getThumbnailKey().isBlank()) {
            thumbnailUrl = s3PresignedUrlProvider.presignGetUrl(t.getThumbnailKey());
        }

        String domain = extractDomain(t.getLink());

        return TaskResponse.builder()
                .taskId(t.getTaskId())
                .collectionId(t.getCollection().getCollectionId())
                .title(t.getTitle())
                .link(t.getLink())
                .memo(t.getMemo())
                .domain(domain)
                .thumbnailUrl(thumbnailUrl)
                .status(t.getStatus())
                .inout(t.getInout())
                .createdAt(t.getCreatedAt())
                .isTutorial(t.getIsTutorial())
                .build();
    }

    private String extractDomain(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = new java.net.URI(link);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            // www. 제거
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            // 매핑
            return switch (host) {
                case "notion.so" -> "노션 (Notion)";
                case "instagram.com" -> "인스타그램 (Instagram)";
                case "youtube.com" -> "유튜브 (YouTube)";
                // 다른 도메인 추가 가능
                default -> "기타";
            };
        } catch (Exception e) {
            return null;
        }
    }
}
