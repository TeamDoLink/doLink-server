package com.doLink_server.domain.collection.dto;

import lombok.Builder;

@Builder
public record CollectionTaskCountResponse(
        Long collectionId,
        Long taskCount
) {
}
