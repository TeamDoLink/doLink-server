package com.doLink_server.domain.collection.dto;

import lombok.Builder;

@Builder
public record CollectionCountResponse(
        Long count
) {
}
