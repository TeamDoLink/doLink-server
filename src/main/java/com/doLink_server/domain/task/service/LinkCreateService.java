package com.doLink_server.domain.task.service;

import com.doLink_server.domain.task.dto.LinkCreateResult;
import com.doLink_server.infra.og.downloader.ImageDownloader;
import com.doLink_server.infra.og.dto.OgMetadata;
import com.doLink_server.infra.og.parser.OgMetadataParser;
import com.doLink_server.infra.og.resizer.OgThumbnailResizer;
import com.doLink_server.infra.s3.OgThumbnailKeyGenerator;
import com.doLink_server.infra.s3.S3PresignedUrlProvider;
import com.doLink_server.infra.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCreateService {

    private final OgMetadataParser ogParser;
    private final ImageDownloader imageDownloader;
    private final OgThumbnailResizer thumbnailResizer;
    private final OgThumbnailKeyGenerator keyGenerator;
    private final S3Uploader s3Uploader;
    private final S3PresignedUrlProvider presignedUrlProvider;

    @Transactional
    public LinkCreateResult create(String url) {
        try {
            // 1) OG 메타 파싱
            OgMetadata og = ogParser.parse(url);
            log.info("[LinkCreate] og.imageUrl={}", og.imageUrl());

            String originalKey = null;
            String originalUrl = null;
            String thumbnailKey = null;
            String thumbnailUrl = null;

            // 2) OG 이미지 처리
            if (og.imageUrl() != null) {
                ImageDownloader.DownloadedImage img = imageDownloader.download(og.imageUrl());

                if (img == null || img.bytes() == null || img.bytes().length == 0) {
                    log.warn("[LinkCreate] image download failed or empty. url={}", og.imageUrl());
                } else {
                    // (A) 원본 업로드
                    originalKey = keyGenerator.generateOriginal(img.contentType());
                    originalKey = s3Uploader.uploadPrivate(originalKey, img.bytes(), img.contentType());
                    originalUrl = presignedUrlProvider.presignGetUrl(originalKey);

                    // (B) 썸네일 생성 + 업로드
                    OgThumbnailResizer.ResizedImage thumb = thumbnailResizer.toSquarePng(img.bytes());
                    if (thumb == null || thumb.bytes() == null || thumb.bytes().length == 0) {
                        log.warn("[LinkCreate] thumbnail resize failed.");
                    } else {
                        thumbnailKey = keyGenerator.generateThumbPng();
                        thumbnailKey = s3Uploader.uploadPrivate(thumbnailKey, thumb.bytes(), thumb.contentType());
                        thumbnailUrl = presignedUrlProvider.presignGetUrl(thumbnailKey);

                        log.info("[LinkCreate] thumbnail uploaded. key={}", thumbnailKey);
                    }
                }
            }

            // 3) 결과 반환 (DB 저장에 필요한 key 포함)
            return new LinkCreateResult(
                    og.title(),
                    og.description(),
                    og.siteName(),
                    og.canonicalUrl(),
                    originalKey,
                    originalUrl,
                    thumbnailKey,
                    thumbnailUrl,
                    og.linkValid()
            );

        } catch (Exception e) {
            log.error("[LinkCreate] failed. url={}", url, e);
            throw e;
        }
    }
}
