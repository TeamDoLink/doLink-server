package com.doLink_server.domain.collection.controller;

import com.doLink_server.domain.collection.dto.CollectionCreateRequest;
import com.doLink_server.domain.collection.dto.CollectionCountResponse;
import com.doLink_server.domain.collection.dto.CollectionCategoryCountResponse;
import com.doLink_server.domain.collection.dto.CollectionDetailResponse;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.dto.CollectionSimpleResponse;
import com.doLink_server.domain.collection.dto.CollectionUpdateRequest;
import com.doLink_server.domain.collection.service.CollectionService;
import com.doLink_server.domain.task.dto.MostTasksCategoryResponse;
import com.doLink_server.global.common.ApiResponse;
import com.doLink_server.global.enums.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 모음 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collect")
@Tag(name = "Collection", description = "모음 관련 API")
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * 모음 생성
     */
    @PostMapping
    @Operation(summary = "모음 생성", description = "새로운 모음을 생성한다.")
    public ApiResponse<CollectionResponse> createCollect(
            @Valid @RequestBody CollectionCreateRequest request) {
        return ApiResponse.onSuccess(collectionService.createCollect(request));
    }

    /**
     * 모음 삭제
     */
    @DeleteMapping("/{collectId}")
    @Operation(summary = "모음 삭제", description = "collectId에 해당하는 모음을 삭제한다.")
    public ApiResponse<String> deleteCollect(@PathVariable("collectId") Long collectId) {
        collectionService.deleteCollect(collectId);
        return ApiResponse.onSuccess("모음 삭제 완료");
    }

    /**
     * 모음 수정
     * - UI: 모음 수정 바텀시트에서 이름/카테고리 수정
     *
     * PATCH /api/v1/collect/{collectId}
     */
    @PatchMapping("/{collectId}")
    @Operation(summary = "모음 수정", description = "collectId에 해당하는 모음의 이름/카테고리를 수정한다.")
    public ApiResponse<CollectionResponse> updateCollect(
            @PathVariable Long collectId,
            @Valid @RequestBody CollectionUpdateRequest request
    ) {
        return ApiResponse.onSuccess(collectionService.updateCollect(collectId, request));
    }

    /**
     * 카테고리별 모음 조회 (무한 스크롤)
     * /api/v1/collect/category?category=FOOD&page=0&size=10
     */
    @GetMapping("/category")
    @Operation(summary = "카테고리별 모음 조회 (무한 스크롤)")
    public ApiResponse<Slice<CollectionResponse>> listByCategory(
            @RequestParam Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(collectionService.listByCategorySlice(category, page, size));
    }

    /**
     * 모음 전체 조회 (무한 스크롤)
     * /api/v1/collect/all?page=0&size=10
     */
    @GetMapping("/all")
    @Operation(summary = "모음 전체 조회 (무한 스크롤)")
    public ApiResponse<Slice<CollectionResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(collectionService.listAllSlice(page, size));
    }

    /**
     * 모음 선택 목록 조회
     * - 할 일 추가 화면에서 "담을 모음 선택"에 사용
     */
    @GetMapping("/select")
    @Operation(
            summary = "모음 선택 목록 조회",
            description = "할 일 추가 화면에서 '담을 모음 선택'에 사용할 모음 목록(id, 이름)을 조회한다."
    )
    public ApiResponse<List<CollectionSimpleResponse>> listCollectSelectOptions() {
        return ApiResponse.onSuccess(collectionService.listCollectOptions());
    }

    /**
     * 모음 상세 조회
     * - 모음의 제목, 카테고리 및 할 일 개수를 조회한다.
     */
    @GetMapping("/{collectId}")
    @Operation(summary = "모음 상세 조회", description = "collectId에 해당하는 모음 상세 정보를 조회한다. 모음의 제목, 카테고리 및 할 일 개수를 조회한다.")
    public ApiResponse<CollectionDetailResponse> getCollectDetail(
            @PathVariable Long collectId
    ) {
        return ApiResponse.onSuccess(collectionService.getCollectDetail(collectId));
    }

    /**
     * 최근 8개 모음 조회
     * - 페이징 없이 로그인 사용자의 최신 생성 순으로 최대 8개의 모음을 반환
     */
    @GetMapping("/top8")
    @Operation(summary = "최근 8개 모음 조회", description = "로그인 사용자의 최근 생성된 모음 8개를 조회한다.")
    public ApiResponse<List<CollectionResponse>> listTop8() {
        return ApiResponse.onSuccess(collectionService.listTopRecentCollections(8));
    }

    /**
     * 전체 모음 개수 조회
     */
    @GetMapping("/count")
    @Operation(summary = "전체 모음 개수 조회", description = "로그인 사용자의 전체 모음 개수를 조회한다.")
    public ApiResponse<CollectionCountResponse> getTotalCollectionCount() {
        return ApiResponse.onSuccess(collectionService.getTotalCollectionCount());
    }

    /**
     * 카테고리별 모음 개수 조회
     */
    @GetMapping("/category-counts")
    @Operation(summary = "카테고리별 모음 개수 조회", description = "로그인 사용자의 카테고리별 모음 개수를 조회한다.")
    public ApiResponse<List<CollectionCategoryCountResponse>> getCategoryCounts() {
        return ApiResponse.onSuccess(collectionService.getCategoryCounts());
    }

    /**
     * 할 일이 가장 많은 모음의 카테고리 조회
     */
    @GetMapping("/most-tasks-category")
    @Operation(summary = "할 일이 가장 많은 모음의 카테고리 조회", description = "로그인 사용자의 모음 중 할 일이 가장 많은 모음의 카테고리를 한국어로 반환한다.")
    public ApiResponse<MostTasksCategoryResponse> getMostTasksCategory() {
        return ApiResponse.onSuccess(collectionService.getMostTasksCategory());
    }

}
