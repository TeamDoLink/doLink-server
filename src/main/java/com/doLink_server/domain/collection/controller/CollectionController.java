package com.doLink_server.domain.collection.controller;

import com.doLink_server.domain.collection.dto.CollectionCreateRequest;
import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.collection.dto.CollectionSimpleResponse;
import com.doLink_server.domain.collection.dto.CollectionUpdateRequest;
import com.doLink_server.domain.collection.service.CollectionService;
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

//
//    /**
//     * 모음 상세 조회
//     */
//    @GetMapping("/{collectId}")
//    @Operation(summary = "모음 상세 조회", description = "collectId에 해당하는 모음 상세 정보를 조회한다.")
//    public ApiResponse<CollectionResponse> get(
//            @PathVariable Long collectId
//    ) {
//        byte[] userId = getCurrentUserId(); // TODO: Security에서 가져오기
//        return ApiResponse.onSuccess(collectionService.get(userId, collectId));
//    }
//

}
