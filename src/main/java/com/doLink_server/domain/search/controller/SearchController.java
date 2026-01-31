package com.doLink_server.domain.search.controller;

import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.search.dto.SearchResponse;
import com.doLink_server.domain.search.service.SearchService;
import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "통합 검색 API")
public class SearchController {

    private final SearchService searchService;

    /**
     * 초기 검색 (모음, 할 일 각각 최대 5개씩)
     * GET /api/v1/search/preview?keyword=검색어
     */
    @GetMapping("/preview")
    @Operation(summary = "초기 검색 미리보기", description = "키워드가 포함된 모음과 할 일을 각각 최대 5개씩 조회한다.")
    public ApiResponse<SearchResponse> searchPreview(
            @Parameter(description = "검색어") @RequestParam String keyword
    ) {
        return ApiResponse.onSuccess(searchService.searchPreview(keyword));
    }

    /**
     * 모음 검색 (이름에 키워드 포함)
     * GET /api/v1/search/collections?keyword=검색어&page=0&size=10
     */
    @GetMapping("/collections")
    @Operation(summary = "모음 검색", description = "키워드가 이름에 포함된 모음을 페이징하여 조회한다.")
    public ApiResponse<Slice<CollectionResponse>> searchCollections(
            @Parameter(description = "검색어") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(searchService.searchCollections(keyword, page, size));
    }

    /**
     * 할 일 검색 (제목에 키워드 포함)
     * GET /api/v1/search/tasks?keyword=검색어&page=0&size=10
     */
    @GetMapping("/tasks")
    @Operation(summary = "할 일 검색", description = "키워드가 제목에 포함된 할 일을 페이징하여 조회한다.")
    public ApiResponse<Slice<TaskResponse>> searchTasks(
            @Parameter(description = "검색어") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(searchService.searchTasks(keyword, page, size));
    }
}
