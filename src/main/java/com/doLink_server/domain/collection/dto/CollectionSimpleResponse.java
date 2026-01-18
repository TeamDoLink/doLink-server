package com.doLink_server.domain.collection.dto;

import lombok.Builder;

/**
 * 모음 선택 UI용 간단 응답 DTO
 * - "할 일 추가" 화면에서 "담을 모음 선택" 목록에 사용
 * - 전체 모음에서 필요한 필드만 내려준다 (id, name)
 */
@Builder
public record CollectionSimpleResponse(
        Long collectionId,
        String name
) {
}
