package com.doLink_server.domain.task.dto;

/**
 * - 링크 생성 결과 DTO
 * - S3 key(저장용)와 presigned URL(표시용)을 함께 반환
 */
public record LinkCreateResult(
        String title,
        String description,
        String siteName,
        String canonicalUrl,
        String originalKey,
        String originalUrl,
        String thumbnailKey,
        String thumbnailUrl,
        boolean linkValid
) {

}

