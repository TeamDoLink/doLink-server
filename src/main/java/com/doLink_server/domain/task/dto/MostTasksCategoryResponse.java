package com.doLink_server.domain.task.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MostTasksCategoryResponse {
    private String categoryKorean;
    private long taskCount;
}
