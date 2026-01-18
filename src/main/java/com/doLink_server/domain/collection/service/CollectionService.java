package com.doLink_server.domain.collection.service;

import com.doLink_server.auth.service.AuthService;
import com.doLink_server.domain.collection.dto.CollectionCreateRequest;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.dto.CollectionSimpleResponse;
import com.doLink_server.domain.collection.dto.CollectionUpdateRequest;
import com.doLink_server.domain.collection.entity.Collection;
import com.doLink_server.domain.collection.repository.CollectionRepository;


import com.doLink_server.global.common.status.ErrorStatus;
import com.doLink_server.global.enums.Category;
import com.doLink_server.global.exception.GeneralException;
import com.doLink_server.user.entity.Users;
import com.doLink_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 모음 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final AuthService authService;     // 로그인 유저 조회
    private final UserService userService;

    /**
     * 모음 전체 조회 (Slice + 최신 생성 순)
     */
    public Slice<CollectionResponse> listAllSlice(int page, int size) {

        Users user = getLoginUser();
        PageRequest pageable = PageRequest.of(page, size);

        return collectionRepository
                .findAllByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toResponse);
    }

    /**
     * 카테고리별 모음 조회 (Slice + 최신 생성 순)
     */
    public Slice<CollectionResponse> listByCategorySlice(
            Category category,
            int page,
            int size
    ) {
        Users user = getLoginUser();
        PageRequest pageable = PageRequest.of(page, size);

        return collectionRepository
                .findAllByUserAndCategoryOrderByCreatedAtDesc(user, category, pageable)
                .map(this::toResponse);
    }

    private Users getLoginUser() {
        String currentUserId = authService.getAuthenticatedUserId();
        return userService.findExistingUser(currentUserId);
    }

    private CollectionResponse toResponse(Collection c) {
        return CollectionResponse.builder()
                .collectionId(c.getCollectionId())
                .name(c.getName())
                .category(c.getCategory())
                .thumbnails(List.of()) // TODO: 썸네일 추후 추가
                .build();
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
                        .build()
        );

        return CollectionResponse.builder()
                .collectionId(saved.getCollectionId()) // 모음 ID
                .name(saved.getName())                 // 모음 이름
                .category(saved.getCategory())         // 모음 카테고리
                .thumbnails(List.of())                 // 썸네일 목록(할 일 기능 이후 채움)
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
        if (!collection.getUser().getUserId().equals(user.getUserId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN_COLLECTION);
        }

        // 4) 삭제
        // CASCADE라 하위 Task도 함께 삭제됨
        collectionRepository.delete(collection);
    }

    /**
     * 모음 수정
     *
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
        if (!collection.getUser().getUserId().equals(user.getUserId())) {
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
                .build();
    }

    /**
     * 할 일 추가 화면용 모음 선택 목록 조회
     *
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
}
