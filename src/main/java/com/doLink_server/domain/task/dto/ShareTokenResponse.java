package com.doLink_server.domain.task.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ShareTokenResponse {
    private String shareToken;
}
