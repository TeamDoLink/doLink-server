package com.doLink_server.domain.collection.dto;

import com.doLink_server.global.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 모음 수정 요청 DTO
 *
 * - 모음 이름(name)과 카테고리(category)를 수정한다.
 * - UI 스펙: 이름 필수(최대 20자), 카테고리 필수
 */
public record CollectionUpdateRequest(
        /**
         * 모음 이름 (필수)
         */
        @NotBlank(message = "모음 이름은 필수입니다.")
        @Size(max = 20, message = "모음 이름은 최대 20자까지 가능합니다.")
        String name,

        /**
         * 모음 카테고리 (필수)
         */
        @NotNull(message = "카테고리는 필수입니다.")
        Category category
) {
}
