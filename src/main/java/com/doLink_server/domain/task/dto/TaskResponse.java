package com.doLink_server.domain.task.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class TaskResponse {
    private Long taskId;
    private Long collectionId;
    private String title;
    private String link;
    private String memo;
    private String domain;
    private String thumbnailUrl;
    private Boolean status;
    private Boolean inout;
    private LocalDateTime createdAt;
    private Boolean isTutorial;
}
