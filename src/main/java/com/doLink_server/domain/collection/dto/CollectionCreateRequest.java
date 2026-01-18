package com.doLink_server.domain.collection.dto;

import com.doLink_server.global.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 모음 생성 요청 DTO
 */
@Builder
public record CollectionCreateRequest(

        @NotBlank
        @Size(max = 20)
        String name,        // 모음 이름(필수, 최대 20자)

        @NotNull
        Category category   // 모음 카테고리(필수)
) {
}
