package com.doLink_server.domain.task.dto;

public record TaskUpdateRequest(
        String title,
        String link,
        String memo,
        Boolean status, // 완료 여부
        Boolean inout   // 내부/외부 여부
) {}
