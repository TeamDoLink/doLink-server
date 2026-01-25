package com.doLink_server.infra.og.dto;

/**
 * OG 이미지 처리 결과 DTO
 *
 * - 원본(original)과 썸네일(thumbnail) 이미지의
 *   S3 key 및 Presigned URL을 함께 전달한다.
 * - 리스트 화면에서는 thumbUrl 위주로 사용한다.
 */
public record OgImageUploadResult(
        String originalKey,
        String thumbKey,
        String originalUrl,
        String thumbUrl
) {
}
