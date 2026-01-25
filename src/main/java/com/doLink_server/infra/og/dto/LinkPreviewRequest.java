package com.doLink_server.infra.og.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkPreviewRequest(
        @NotBlank String url
) {
}
