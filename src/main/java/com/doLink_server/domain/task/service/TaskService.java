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
        String finalTitle = request.title(); // 사용자가 직접 입력한 제목
        String ogImageKey = null;
        String thumbnailKey = null;

        if (request.link() != null && !request.link().isBlank()) {
            LinkCreateResult linkResult = linkCreateService.create(request.link());

            ogImageKey = linkResult.originalKey();
            thumbnailKey = linkResult.thumbnailKey();

            // 사용자가 제목을 입력하지 않았다면 OG 제목을 정제해서 사용
            if (finalTitle == null || finalTitle.isBlank()) {
                finalTitle = refineTitle(linkResult.title());
            }
        }

        // 5) Task 저장 (thumbnailKey 포함)
        Task saved = taskRepository.save(
                Task.builder()
                        .user(user)
                        .collection(collection)
                        .title(finalTitle)
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

    private String refineTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) return "제목 없음";

        // 1) 줄바꿈 문자(\n, \r)를 공백으로 치환하거나 첫 줄만 가져오기
        String refined = rawTitle.split("\n")[0];

        // 2) 앞뒤 공백 제거 및 불필요한 따옴표 제거
        refined = refined.replace("\"", "").replace("'", "").trim();

        // 3) 너무 길면 적당히 자르기 (예: 50자)
        if (refined.length() > 50) {
            refined = refined.substring(0, 47) + "...";
        }

        return refined;
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

        // status 파라미터에 false를 넣어서 호출
        return taskRepository.findByUserAndStatus(user, false, pageable)
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

            host = host.toLowerCase();

            // www. 제거
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return switch (host) {
                // --- 소셜(SNS) ---
                case "linkedin.com" -> "링크드인 (LinkedIn)";
                case "threads.net" -> "스레드 (Threads)";
                case "x.com", "t.co" -> "엑스 (X)";
                case "instagram.com" -> "인스타그램 (Instagram)";
                case "story.kakao.com" -> "카카오스토리 (KakaoStory)";
                case "facebook.com" -> "페이스북 (Facebook)";

                // --- 숏폼 ---
                case "douyin.com" -> "더우인 (Douyin)";
                case "tiktok.com" -> "틱톡 (TikTok)";

                // --- 레퍼런스 ---
                case "pinterest.com" -> "핀터레스트 (Pinterest)";

                // --- 동영상/스트리밍 ---
                case "tv.naver.com" -> "네이버 TV (NAVER TV)";
                case "youtube.com" -> "유튜브 (YouTube)";
                case "chzzk.naver.com" -> "치지직 (CHZZK)";
                case "twitch.tv" -> "트위치 (Twitch)";

                // --- 커뮤니티/카페 ---
                case "cafe.naver.com" -> "네이버 카페 (Naver Cafe)";
                case "cafe.daum.net" -> "다음 카페 (Daum Cafe)";
                case "band.us" -> "밴드 (BAND)";

                // --- 개발/문서 ---
                case "gitlab.com" -> "깃랩 (GitLab)";
                case "github.com" -> "깃허브 (GitHub)";
                case "stackoverflow.com" -> "스택오버플로 (Stack Overflow)";

                // --- 뉴스 ---
                case "news.google.com" -> "구글 뉴스 (Google News)";
                case "news.naver.com" -> "네이버 뉴스 (Naver News)";
                case "news.daum.net" -> "다음 뉴스 (Daum News)";

                // --- 기존 ---
                case "notion.so" -> "노션 (Notion)";

                default -> "기타";
            };
        } catch (Exception e) {
            return null;
        }
    }
}
