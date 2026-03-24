package com.doLink_server.infra.og.dto;

/**
 * - 외부 URL 페이지의 Open Graph(OG) 메타데이터 파싱 결과 DTO
 * - 링크 저장 시 미리보기 정보(제목, 설명, 썸네일 등) 전달 목적
 */
public record OgMetadata(
        String title,
        String description,
        String imageUrl,      // 원본 og:image (외부 URL)
        String siteName,
        String canonicalUrl,  // og:url 또는 canonical
        boolean linkValid     // URL 접속 성공 여부
) {
    public static OgMetadata invalid() {
        return new OgMetadata(null, null, null, null, null, false);
    }
}
