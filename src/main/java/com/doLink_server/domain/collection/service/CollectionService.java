package com.doLink_server.domain.collection.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.dto.CollectionCategoryCountResponse;
import com.doLink_server.domain.collection.dto.CollectionCountResponse;
import com.doLink_server.domain.collection.dto.CollectionCreateRequest;
import com.doLink_server.domain.collection.dto.CollectionDetailResponse;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.dto.CollectionSimpleResponse;
import com.doLink_server.domain.collection.dto.CollectionTaskCountResponse;
import com.doLink_server.domain.collection.dto.CollectionUpdateRequest;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;
import com.doLink_server.domain.task.dto.MostTasksCategoryResponse;
import com.doLink_server.domain.task.repository.CollectionTaskCountRow;
import com.doLink_server.domain.task.repository.CollectionThumbnailRow;
import com.doLink_server.domain.task.repository.TaskRepository;
import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.enums.Category;
import com.doLink_server.global.exception.GeneralException;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 모음 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final TaskRepository taskRepository;
    private final S3PresignedUrlProvider presignedUrlProvider;
    private final AuthService authService;     // 로그인 유저 조회
    private final UserService userService;

    /**
     * 모음 전체 조회 (Slice + 최신 생성 순) + 썸네일(최대 4개) 포함
     */
    public Slice<CollectionResponse> listAllSlice(int page, int size) {

        Users user = getLoginUser();
        PageRequest pageable = PageRequest.of(page, size);

        Slice<Collection> slice =
                collectionRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        List<Collection> collections = slice.getContent();
        if (collections.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, slice.hasNext());
        }

        // 1) 모음 ID 수집
        List<Long> collectionIds = collections.stream()
                .map(Collection::getCollectionId)
                .toList();

        // 2) 모음별 썸네일 key를 한 번에 조회 (모음당 최대 4개)
        List<CollectionThumbnailRow> rows =
                taskRepository.findTop4ThumbnailsByCollectionIds(collectionIds);

        // 3) collectionId -> thumbnailKey 리스트로 그룹핑
        Map<Long, List<String>> thumbKeyMap = rows.stream()
                .collect(Collectors.groupingBy(
                        CollectionThumbnailRow::getCollectionId,
                        Collectors.mapping(
                                CollectionThumbnailRow::getThumbnailKey,
                                Collectors.toList()
                        )
                ));

        // 4) 모음별 할 일 개수 조회
        List<CollectionTaskCountRow> countRows = taskRepository.countByCollectionIds(collectionIds);
        Map<Long, Long> taskCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        CollectionTaskCountRow::getCollectionId,
                        CollectionTaskCountRow::getTaskCount
                ));

        // 5) presigned URL 변환 + DTO 생성
        List<CollectionResponse> responses = collections.stream()
                .map(c -> {
                    List<String> thumbnailUrls = thumbKeyMap
                            .getOrDefault(c.getCollectionId(), List.of())
                            .stream()
                            .map(presignedUrlProvider::presignGetUrl)
                            .toList();

                    int taskCount = taskCountMap.getOrDefault(c.getCollectionId(), 0L).intValue();

                    return CollectionResponse.builder()
                            .collectionId(c.getCollectionId())
                            .name(c.getName())
                            .category(c.getCategory())
                            .thumbnails(thumbnailUrls)
                            .taskCount(taskCount)
                            .isTutorial(c.getIsTutorial())
                            .build();
                })
                .toList();

        return new SliceImpl<>(responses, pageable, slice.hasNext());
    }

    /**
     * 카테고리별 모음 조회 (Slice + 최신 생성 순) + 썸네일(최대 4개) 포함
     */
    public Slice<CollectionResponse> listByCategorySlice(Category category, int page, int size) {

        Users user = getLoginUser();
        PageRequest pageable = PageRequest.of(page, size);

        Slice<Collection> slice =
                collectionRepository.findAllByUserAndCategoryOrderByCreatedAtDesc(user, category, pageable);

        List<Collection> collections = slice.getContent();
        if (collections.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, slice.hasNext());
        }

        // 1) 모음 ID 수집
        List<Long> collectionIds = collections.stream()
                .map(Collection::getCollectionId)
                .toList();

        // 2) 모음별 썸네일 key를 한 번에 조회 (모음당 최대 4개)
        List<CollectionThumbnailRow> rows =
                taskRepository.findTop4ThumbnailsByCollectionIds(collectionIds);

        // 3) collectionId -> thumbnailKey 리스트로 그룹핑
        Map<Long, List<String>> thumbKeyMap = rows.stream()
                .collect(Collectors.groupingBy(
                        CollectionThumbnailRow::getCollectionId,
                        Collectors.mapping(
                                CollectionThumbnailRow::getThumbnailKey,
                                Collectors.toList()
                        )
                ));

        // 4) 모음별 할 일 개수 조회
        List<CollectionTaskCountRow> countRows = taskRepository.countByCollectionIds(collectionIds);
        Map<Long, Long> taskCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        CollectionTaskCountRow::getCollectionId,
                        CollectionTaskCountRow::getTaskCount
                ));

        // 5) presigned URL 변환 + DTO 생성
        List<CollectionResponse> responses = collections.stream()
                .map(c -> {
                    List<String> thumbnailUrls = thumbKeyMap
                            .getOrDefault(c.getCollectionId(), List.of())
                            .stream()
                            .map(presignedUrlProvider::presignGetUrl)
                            .toList();

                    int taskCount = taskCountMap.getOrDefault(c.getCollectionId(), 0L).intValue();

                    return CollectionResponse.builder()
                            .collectionId(c.getCollectionId())
                            .name(c.getName())
                            .category(c.getCategory())
                            .thumbnails(thumbnailUrls)
                            .taskCount(taskCount)
                            .isTutorial(c.getIsTutorial())
                            .build();
                })
                .toList();

        return new SliceImpl<>(responses, pageable, slice.hasNext());
    }

    private Users getLoginUser() {
        String currentUserId = authService.getAuthenticatedUserId();
        return userService.findExistingUser(currentUserId);
    }

    /**
     * 모음 생성
     */
    @Transactional
    public CollectionResponse createCollect(CollectionCreateRequest request) {

        // 1) 유저 조회
        String currentUserId = authService.getAuthenticatedUserId(); // 인증객체에서 사용자 아이디(UUID 문자열) 가져오기
        Users user = userService.findExistingUser(currentUserId); // 유저 존재 검증 (없으면 예외)

        Collection saved = collectionRepository.save(
                Collection.builder()
                        .user(user)
                        .name(request.name())
                        .category(request.category())
                        .isTutorial(false)
                        .build()
        );

        return CollectionResponse.builder()
                .collectionId(saved.getCollectionId()) // 모음 ID
                .name(saved.getName())                 // 모음 이름
                .category(saved.getCategory())         // 모음 카테고리
                .thumbnails(List.of())                 // 썸네일 목록(할 일 기능 이후 채움)
                .taskCount(0)                          // 새 모음은 할 일 없음
                .isTutorial(saved.getIsTutorial())
                .build();
    }

    /**
     * 모음 삭제
     * - 로그인 사용자의 모음만 삭제 가능
     * - (DB FK ON DELETE CASCADE 설정 시) 하위 Task도 함께 삭제됨
     */
    @Transactional
    public void deleteCollect(Long collectionId) {

        // 1) 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);


        // 2) 모음 조회 (User까지 fetch join) - 없으면 404
        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 3) 권한 확인 - 남의 모음이면 403
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 튜토리얼은 삭제 불가능
        if (Boolean.TRUE.equals(collection.getIsTutorial())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 4) 삭제
        // CASCADE라 하위 Task도 함께 삭제됨
        collectionRepository.delete(collection);
    }

    /**
     * 모음 수정
     * <p>
     * - 로그인 사용자의 모음만 수정 가능
     * - 수정 항목: name, category
     *
     * @param collectionId 수정할 모음 ID
     * @param request      수정 요청(name, category)
     * @return 수정된 모음 응답 DTO
     */
    @Transactional
    public CollectionResponse updateCollect(Long collectionId, CollectionUpdateRequest request) {

        // 1) 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2) 모음 조회 + user fetch join (없으면 404)
        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 3) 권한 체크 (내 모음인지)
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 튜토리얼은 수정 불가능
        if (Boolean.TRUE.equals(collection.getIsTutorial())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 4) 수정 반영 (Dirty Checking)
        collection.setName(request.name());
        collection.setCategory(request.category());

        // 5) 응답 변환
        return CollectionResponse.builder()
                .collectionId(collection.getCollectionId())
                .name(collection.getName())
                .category(collection.getCategory())
                .thumbnails(List.of()) // TODO: Task 썸네일 완성되면 채우기
                .taskCount(0) // TODO: Task 개수 계산 필요 시 추가
                .isTutorial(collection.getIsTutorial())
                .build();
    }

    /**
     * 할 일 추가 화면용 모음 선택 목록 조회
     * <p>
     * - 로그인한 사용자가 보유한 모음 전체 조회
     * 사용처: 할 일 추가 화면 > "담을 모음 선택"
     */
    @Transactional(readOnly = true)
    public List<CollectionSimpleResponse> listCollectOptions() {
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        return collectionRepository.findAllByUser(user)
                .stream()
                .map(c -> CollectionSimpleResponse.builder()
                        .collectionId(c.getCollectionId())
                        .name(c.getName())
                        .build())
                .toList();
    }

    /**
     * 모음 상세 조회 (모음의 제목, 카테고리 및 최근 tasks)
     *
     * @param collectionId 모음 ID
     * @return CollectionDetailResponse
     */
    @Transactional(readOnly = true)
    public CollectionDetailResponse getCollectDetail(Long collectionId) {
        // 1) 로그인 유저 조회
        String currentUserId = authService.getAuthenticatedUserId();
        Users user = userService.findExistingUser(currentUserId);

        // 2) 모음 조회 + user fetch join (없으면 404)
        Collection collection = collectionRepository.findByIdWithUser(collectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND_COLLECTION));

        // 3) 권한 확인 - 남의 모음이면 403
        if (!Arrays.equals(collection.getUser().getUserId(), user.getUserId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 5) 해당 모음의 총 할 일 개수 조회
        long totalCount = taskRepository.countByCollection_CollectionId(collectionId);

        // 6) CollectionDetailResponse에는 최근 tasks 없이 총 개수만 반환
        return CollectionDetailResponse.builder()
                .collectionId(collection.getCollectionId())
                .name(collection.getName())
                .category(collection.getCategory())
                .taskCount((int) totalCount)
                .build();
    }

    /**
     * 최근 N개 모음 조회 (페이징 없이, 최신 생성 순)
     * - 기본 사용: 최근 8개 조회
     */
    @Transactional(readOnly = true)
    public List<CollectionResponse> listTopRecentCollections(int limit) {
        Users user = getLoginUser();
        // 0번째 페이지에 limit 크기의 요청을 만들어 최신 생성순으로 조회
        PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<Collection> slice = collectionRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);
        List<Collection> collections = slice.getContent();
        if (collections.isEmpty()) {
            return List.of();
        }

        // 1) 모음 ID 수집
        List<Long> collectionIds = collections.stream()
                .map(Collection::getCollectionId)
                .toList();

        // 2) 모음별 썸네일 key를 한 번에 조회 (모음당 최대 4개)
        List<CollectionThumbnailRow> rows =
                taskRepository.findTop4ThumbnailsByCollectionIds(collectionIds);

        // 3) collectionId -> thumbnailKey 리스트로 그룹핑
        Map<Long, List<String>> thumbKeyMap = rows.stream()
                .collect(Collectors.groupingBy(
                        CollectionThumbnailRow::getCollectionId,
                        Collectors.mapping(
                                CollectionThumbnailRow::getThumbnailKey,
                                Collectors.toList()
                        )
                ));

        // 4) 모음별 할 일 개수 조회
        List<CollectionTaskCountRow> countRows = taskRepository.countByCollectionIds(collectionIds);
        Map<Long, Long> taskCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        CollectionTaskCountRow::getCollectionId,
                        CollectionTaskCountRow::getTaskCount
                ));

        // 5) presigned URL 변환 + DTO 생성

        return collections.stream()
                .map(c -> {
                    List<String> thumbnailUrls = thumbKeyMap
                            .getOrDefault(c.getCollectionId(), List.of())
                            .stream()
                            .map(presignedUrlProvider::presignGetUrl)
                            .toList();

                    int taskCount = taskCountMap.getOrDefault(c.getCollectionId(), 0L).intValue();

                    return CollectionResponse.builder()
                            .collectionId(c.getCollectionId())
                            .name(c.getName())
                            .category(c.getCategory())
                            .thumbnails(thumbnailUrls)
                            .taskCount(taskCount)
                            .isTutorial(c.getIsTutorial())
                            .build();
                })
                .toList();
    }

    /**
     * 전체 모음 개수 조회
     */
    public CollectionCountResponse getTotalCollectionCount() {
        Long count = collectionRepository.count();
        return CollectionCountResponse.builder()
                .count(count)
                .build();
    }

    /**
     * 카테고리별 모음 개수 조회
     */
    public List<CollectionCategoryCountResponse> getCategoryCounts() {
        Users user = getLoginUser();
        return Arrays.stream(Category.values())
                .map(category -> {
                    long count = collectionRepository.countByUserAndCategory(user, category);
                    return CollectionCategoryCountResponse.builder()
                            .categoryKorean(category.getLabelKorean())
                            .count(count)
                            .build();
                })
                .toList();
    }

    /**
     * 할 일이 가장 많은 모음의 카테고리 조회
     * - 카테고리별 할 일 합산 후, ETC(기타)는 -1 페널티를 적용하여 비교
     * - 동점일 경우 ETC가 아닌 카테고리 우선, 그 외에는 먼저 등장한 카테고리 반환
     */
    public MostTasksCategoryResponse getMostTasksCategory() {
        Users user = getLoginUser();

        List<Collection> collections = collectionRepository.findAllByUser(user);
        if (collections.isEmpty()) {
            return createZeroEtcResponse();
        }

        List<Long> collectionIds = collections.stream()
                .map(Collection::getCollectionId)
                .toList();

        List<CollectionTaskCountRow> countRows = taskRepository.countByCollectionIds(collectionIds);
        if (countRows.isEmpty()) {
            return createZeroEtcResponse();
        }

        // 1. 카테고리별 점수 합산 (ETC는 처음부터 -1 처리)
        Map<Category, Long> categoryScoreMap = new java.util.EnumMap<>(Category.class);

        // 초기값 세팅 (ETC는 기본적으로 -1부터 시작하거나 나중에 뺌)
        for (CollectionTaskCountRow row : countRows) {
            Collection c = collectionRepository.findById(row.getCollectionId()).orElse(null);
            if (c == null) continue;
            categoryScoreMap.merge(c.getCategory(), row.getTaskCount(), Long::sum);
        }

        // 2. ETC 카테고리의 값을 항상 -1 차감 (사용자 요구사항 반영)
        categoryScoreMap.computeIfPresent(Category.ETC, (k, v) -> v - 1);

        // 만약 ETC가 1개였는데 -1해서 0이 되었고, 다른 카테고리가 아예 없다면?
        // 혹은 결과가 마이너스가 되는 것을 방지하려면 Math.max(0, v - 1) 사용 가능
        if (categoryScoreMap.isEmpty()) {
            return createZeroEtcResponse();
        }

        // 3. 최적화된 Max 탐색 (이미 점수에 페널티가 반영되어 있으므로 단순 비교)
        Category bestCategory = categoryScoreMap.entrySet().stream()
                .max((e1, e2) -> {
                    int compare = Long.compare(e1.getValue(), e2.getValue());
                    if (compare == 0) {
                        // 점수 동점일 때 ETC는 후순위로 밀림
                        if (e1.getKey() == Category.ETC) return -1;
                        if (e2.getKey() == Category.ETC) return 1;
                    }
                    return compare;
                })
                .map(Map.Entry::getKey)
                .orElse(Category.ETC);

        // 4. 이미 계산된(차감된) 점수를 그대로 반환
        long finalCount = categoryScoreMap.getOrDefault(bestCategory, 0L);

        return MostTasksCategoryResponse.builder()
                .categoryKorean(bestCategory.getLabelKorean())
                .taskCount(Math.max(0, finalCount)) // 음수 방지
                .build();
    }

    // 중복 코드 공통화
    private MostTasksCategoryResponse createZeroEtcResponse() {
        return MostTasksCategoryResponse.builder()
                .categoryKorean(Category.ETC.getLabelKorean())
                .taskCount(0)
                .build();
    }

    /**
     * 각 모음의 할 일 전체 개수 조회
     */
    public List<CollectionTaskCountResponse> getTaskCountsForCollections() {
        Users user = getLoginUser();

        List<Collection> collections = collectionRepository.findAllByUser(user);
        if (collections.isEmpty()) {
            return List.of();
        }

        List<Long> collectionIds = collections.stream()
                .map(Collection::getCollectionId)
                .toList();

        List<CollectionTaskCountRow> countRows = taskRepository.countByCollectionIds(collectionIds);

        Map<Long, Long> taskCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        CollectionTaskCountRow::getCollectionId,
                        CollectionTaskCountRow::getTaskCount
                ));

        return collections.stream()
                .map(c -> CollectionTaskCountResponse.builder()
                        .collectionId(c.getCollectionId())
                        .taskCount(taskCountMap.getOrDefault(c.getCollectionId(), 0L))
                        .build())
                .toList();
    }
}
