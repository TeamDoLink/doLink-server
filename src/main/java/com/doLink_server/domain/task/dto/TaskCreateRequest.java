package com.doLink_server.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest(

        /** 모음 ID */
        @NotNull(message = "컬렉션 ID는 필수입니다.")
        Long collectionId,

        /** 할 일 제목 */
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 최대 100자까지 가능합니다.")
        String title,

        String link,
        String memo,
        Boolean inout
) {
}
