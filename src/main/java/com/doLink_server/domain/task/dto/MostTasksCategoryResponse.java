package com.doLink_server.domain.task.dto;

import lombok.Builder;

@Builder
public record MostTasksCategoryResponse (
     String categoryKorean,
     long taskCount
) {
}
