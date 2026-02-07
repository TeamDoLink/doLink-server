package com.doLink_server.domain.task.dto;

public record TaskUpdateRequest(
        Long collectionId, // 모음 ID
        String title,
        String link,
        String memo
) {}
