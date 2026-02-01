package com.doLink_server.domain.search.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.search.dto.SearchResponse;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.domain.task.entity.Task;
import com.doLink_server.domain.task.repository.TaskRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final AuthService authService;
    private final UserService userService;
    private final CollectionRepository collectionRepository;
    private final TaskRepository taskRepository;

    /**
     * 초기 검색 미리보기 (모음, 할 일 각각 최대 5개)
     * - 최신순(created_at DESC) 정렬
     */
    public SearchResponse searchPreview(String keyword) {
        // 1. 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2. 페이징 설정 (최신순, 최대 5개)
        PageRequest pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. 키워드가 없거나 공백이면 빈 List 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return SearchResponse.builder()
                    .collections(Collections.emptyList())
                    .tasks(Collections.emptyList())
                    .hasMoreCollections(false)
                    .hasMoreTasks(false)
                    .build();
        }

        // 4. 모음 이름 검색 (최대 5개)
        Slice<Collection> collectionSlice = collectionRepository.findByUserAndNameContaining(user, keyword, pageable);
        List<CollectionResponse> collectionResponses = collectionSlice.getContent().stream()
                .map(c -> CollectionResponse.builder()
                        .collectionId(c.getCollectionId())
                        .name(c.getName())
                        .category(c.getCategory())
                        .thumbnails(List.of())
                        .build())
                .toList();

        // 5. 할 일 제목 검색 (최대 5개)
        Slice<Task> taskSlice = taskRepository.findByUserAndTitleContaining(user, keyword, pageable);
        List<TaskResponse> taskResponses = taskSlice.getContent().stream()
                .map(t -> TaskResponse.builder()
                        .taskId(t.getTaskId())
                        .collectionId(t.getCollection().getCollectionId())
                        .title(t.getTitle())
                        .link(t.getLink())
                        .memo(t.getMemo())
                        .status(t.getStatus())
                        .inout(t.getInout())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();

        // 6. 결과 반환 (hasNext()로 더 많은 결과가 있는지 확인)
        return SearchResponse.builder()
                .collections(collectionResponses)
                .tasks(taskResponses)
                .hasMoreCollections(collectionSlice.hasNext())
                .hasMoreTasks(taskSlice.hasNext())
                .build();
    }

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

        // 5. DTO 변환 후 반환
        return collectionSlice.map(c -> CollectionResponse.builder()
                .collectionId(c.getCollectionId())
                .name(c.getName())
                .category(c.getCategory())
                .thumbnails(List.of()) // 검색 목록에서는 썸네일 생략 (성능 최적화)
                .build());
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
        return taskSlice.map(t -> TaskResponse.builder()
                .taskId(t.getTaskId())
                .collectionId(t.getCollection().getCollectionId())
                .title(t.getTitle())
                .link(t.getLink())
                .memo(t.getMemo())
                .status(t.getStatus())
                .inout(t.getInout())
                .createdAt(t.getCreatedAt())
                .build());
    }
}
