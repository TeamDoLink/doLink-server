package com.doLink_server.domain.search.dto;

import com.doLink_server.domain.collection.dto.CollectionResponse;
import com.doLink_server.domain.task.dto.TaskResponse;
import lombok.Builder;
import org.springframework.data.domain.Slice;

/**
 * 통합 검색 결과 응답 DTO
 */
@Builder
public record SearchResponse(
        Slice<CollectionResponse> collections, // 검색된 모음 목록
        Slice<TaskResponse> tasks              // 검색된 할 일 목록
) {
}
