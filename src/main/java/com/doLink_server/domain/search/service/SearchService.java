package com.doLink_server.domain.search.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.search.dto.SearchResponse;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.entity.Task;
import com.doLink_server.domain.task.repository.CollectionThumbnailRow;
import com.doLink_server.domain.task.repository.TaskRepository;
import com.doLink_server.infra.s3.S3PresignedUrlProvider;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final AuthService authService;
    private final UserService userService;
    private final CollectionRepository collectionRepository;
    private final TaskRepository taskRepository;
    private final S3PresignedUrlProvider s3PresignedUrlProvider;

    /**
     * 모음 검색 (이름에 키워드 포함, 페이징)
     * - 최신순(created_at DESC) 정렬
     */
    public Slice<CollectionResponse> searchCollections(String keyword, int page, int size) {
        // 1. 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2. 페이징 설정 (최신순)
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. 키워드가 없거나 공백이면 빈 Slice 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        // 4. 모음 이름 검색
        Slice<Collection> collectionSlice = collectionRepository.findByUserAndNameContaining(user, keyword, pageable);
        List<Collection> collections = collectionSlice.getContent();

        // 5. 썸네일 포함하여 DTO 변환
        List<CollectionResponse> collectionResponses;
        if (!collections.isEmpty()) {
            List<Long> collectionIds = collections.stream()
                    .map(Collection::getCollectionId)
                    .toList();

            List<CollectionThumbnailRow> thumbnailRows =
                    taskRepository.findTop4ThumbnailsByCollectionIds(collectionIds);

            Map<Long, List<String>> thumbKeyMap = thumbnailRows.stream()
                    .collect(Collectors.groupingBy(
                            CollectionThumbnailRow::getCollectionId,
                            Collectors.mapping(
                                    CollectionThumbnailRow::getThumbnailKey,
                                    Collectors.toList()
                            )
                    ));

            collectionResponses = collections.stream()
                    .map(c -> {
                        List<String> thumbnailUrls = thumbKeyMap
                                .getOrDefault(c.getCollectionId(), List.of())
                                .stream()
                                .map(s3PresignedUrlProvider::presignGetUrl)
                                .toList();

                        return CollectionResponse.builder()
                                .collectionId(c.getCollectionId())
                                .name(c.getName())
                                .category(c.getCategory())
                                .thumbnails(thumbnailUrls)
                                .build();
                    })
                    .toList();
        } else {
            collectionResponses = Collections.emptyList();
        }

        return new SliceImpl<>(collectionResponses, pageable, collectionSlice.hasNext());
    }

    /**
     * 할 일 검색 (제목에 키워드 포함, 페이징)
     * - 최신순(created_at DESC) 정렬
     */
    public Slice<TaskResponse> searchTasks(String keyword, int page, int size) {
        // 1. 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2. 페이징 설정 (최신순)
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. 키워드가 없거나 공백이면 빈 Slice 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        // 4. 할 일 제목 검색
        Slice<Task> taskSlice = taskRepository.findByUserAndTitleContaining(user, keyword, pageable);

        // 5. DTO 변환 후 반환
        return taskSlice.map(t -> {
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
        });
    }

    /**
     * Task를 TaskResponse로 변환하는 헬퍼 메서드
     */
    private TaskResponse toTaskResponse(Task t) {
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
