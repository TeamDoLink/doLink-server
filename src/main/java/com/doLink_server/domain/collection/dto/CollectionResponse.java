package com.doLink_server.domain.collection.dto;

import com.doLink_server.global.enums.Category;
import lombok.Builder;

import java.util.List;

/**
 * 모음 응답 DTO
 */
@Builder
public record CollectionResponse (
        Long collectionId,     // 모음 ID
        String name,           // 모음 이름
        Category category,     // 모음 카테고리
        List<String> thumbnails, // 모음에 속한 할 일들의 대표 썸네일 URL 목록 (최대 4개)
        int taskCount,         // 모음에 속한 할 일 개수
        Boolean isTutorial     // 튜토리얼 여부
){
}
