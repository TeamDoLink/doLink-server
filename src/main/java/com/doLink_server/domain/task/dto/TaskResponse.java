package com.doLink_server.domain.task.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TaskResponse(
        Long taskId,
        Long collectionId,
        String title,
        String link,
        String memo,
        Boolean status,
        Boolean inout,
        LocalDateTime createdAt
) {
}
