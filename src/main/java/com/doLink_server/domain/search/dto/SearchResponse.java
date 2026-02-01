package com.doLink_server.domain.search.dto;

import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.task.dto.TaskResponse;
import lombok.Builder;

import java.util.List;

/**
 * 통합 검색 결과 응답 DTO
 * - 각각 최대 5개까지 반환
 */
@Builder
public record SearchResponse(
        List<CollectionResponse> collections, // 검색된 모음 목록 (최대 5개)
        List<TaskResponse> tasks,             // 검색된 할 일 목록 (최대 5개)
        Boolean hasMoreCollections,           // 모음이 5개보다 더 많은지 여부
        Boolean hasMoreTasks                  // 할 일이 5개보다 더 많은지 여부
) {
}
