package com.doLink_server.infra.og.processor;

import com.doLink_server.infra.og.downloader.ImageDownloader;
import com.doLink_server.infra.og.dto.OgImageUploadResult;
import com.doLink_server.infra.og.resizer.OgThumbnailResizer;
import com.doLink_server.infra.s3.OgThumbnailKeyGenerator;
import com.doLink_server.infra.s3.S3PresignedUrlProvider;
import com.doLink_server.infra.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OG 이미지 처리 오케스트레이션 컴포넌트.
 *
 * 흐름:
 * 1. OG 이미지 다운로드
 * 2. 원본 이미지 S3 업로드
 * 3. 썸네일 생성 및 업로드
 * 4. Presigned URL 발급
 */
@Component
@RequiredArgsConstructor
public class OgImageProcessor {

    private final ImageDownloader downloader;
    private final OgThumbnailResizer resizer;
    private final OgThumbnailKeyGenerator keyGenerator;
    private final S3Uploader uploader;
    private final S3PresignedUrlProvider presigner;

    /**
     * @param imageUrl OG 이미지 URL
     * @return 원본/썸네일 S3 key 및 Presigned URL
     */
    public OgImageUploadResult process(String imageUrl) {
        var downloaded = downloader.download(imageUrl);
        if (downloaded == null) return null;

        // 1) 원본 업로드
        String originalKey = keyGenerator.generateOriginal(downloaded.contentType());
        uploader.uploadPrivate(originalKey, downloaded.bytes(), downloaded.contentType());

        // 2) 썸네일 생성
        var thumb = resizer.toSquarePng(downloaded.bytes());
        if (thumb == null) {
            // 썸네일 실패 시 원본만 반환
            return new OgImageUploadResult(
                    originalKey,
                    null,
                    presigner.presignGetUrl(originalKey),
                    null
            );
        }

        // 3) 썸네일 업로드
        String thumbKey = keyGenerator.generateThumbPng();
        uploader.uploadPrivate(thumbKey, thumb.bytes(), thumb.contentType());

        // 4) Presigned URL 발급
        return new OgImageUploadResult(
                originalKey,
                thumbKey,
                presigner.presignGetUrl(originalKey),
                presigner.presignGetUrl(thumbKey)
        );
    }
}
