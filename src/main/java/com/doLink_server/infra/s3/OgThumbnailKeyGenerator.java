package com.doLink_server.infra.s3;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * - OG 썸네일 저장용 S3 key 생성
 * - 날짜 prefix + UUID 기반으로 충돌 없이 생성
 */
@Component
public class OgThumbnailKeyGenerator {

    /**
     * OG 이미지 원본(original)을 저장하기 위한 S3 Key 생성
     * 예시: og-image/original/2025/12/17/uuid.jpg
    */
    public String generateOriginal(String contentType) {
        String ext = toExt(contentType);
        return gen("og-image/original", ext);
    }

    /**
     * OG 이미지 썸네일(thumbnail)을 저장하기 위한 S3 Key 생성
     *
     * - 리스트/모아보기 화면에서 빠른 로딩과 트래픽 절감을 목적.
     *  * 예시: og-image/thumb/2025/12/17/uuid.png
     */
    public String generateThumbPng() {
        return gen("og-image/thumb", "png");
    }

    /**
     * 공통 S3 Key 생성 로직
     * 구조: {prefix}/{yyyy}/{MM}/{dd}/{uuid}.{ext}
     *
     * @param prefix S3 객체 상위 경로
     * @param ext    파일 확장자
     * @return 완성된 S3 key
     */
    private String gen(String prefix, String ext) {
        LocalDate d = LocalDate.now();
        return String.format("%s/%04d/%02d/%02d/%s.%s",
                prefix, d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                UUID.randomUUID(), ext);
    }

    private String toExt(String contentType) {
        if (contentType == null) return "bin";
        String ct = contentType.toLowerCase();
        if (ct.contains("jpeg")) return "jpg";
        if (ct.contains("png"))  return "png";
        if (ct.contains("webp")) return "webp";
        if (ct.contains("gif"))  return "gif";
        return "bin";
    }

}
