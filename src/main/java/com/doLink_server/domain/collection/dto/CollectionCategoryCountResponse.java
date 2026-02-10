package com.doLink_server.domain.collection.dto;

import lombok.Builder;

@Builder
public record CollectionCategoryCountResponse(
        String categoryKorean,
        Long count
) {
}
