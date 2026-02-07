package com.doLink_server.domain.collection.dto;

import com.doLink_server.domain.task.dto.TaskResponse;
import com.doLink_server.global.enums.Category;
import lombok.Builder;

import java.util.List;

@Builder
public record CollectionDetailResponse(
        Long collectionId,
        String name,
        Category category,
        Integer taskCount
) {
}
