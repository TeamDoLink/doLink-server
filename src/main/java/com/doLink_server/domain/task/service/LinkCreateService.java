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

/**
 * 링크 생성 시 OG 메타데이터를 파싱하고
 * 대표 썸네일을 생성하여 S3에 저장하는 서비스
 *
 * - 썸네일: 256x256 cover crop + webp(quality 0.85)
 * - DB에는 S3 key 저장
 * - 응답에는 presigned URL 포함
 */
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
            // 1️⃣ OG 메타데이터 파싱
            OgMetadata og = ogParser.parse(url);
            log.info("[LinkCreate] og.imageUrl={}", og.imageUrl());

            String thumbnailKey = null;
            String thumbnailUrl = null;

            // 2️⃣ OG 이미지가 있는 경우에만 처리
            if (og.imageUrl() != null) {

                ImageDownloader.DownloadedImage img =
                        imageDownloader.download(og.imageUrl());

                if (img == null) {
                    log.warn("[LinkCreate] image download failed. url={}", og.imageUrl());
                }

                if (img != null && img.bytes() != null && img.bytes().length > 0) {

                    // 3️⃣ 썸네일 리사이즈
                    OgThumbnailResizer.ResizedImage thumb =
                            thumbnailResizer.toSquarePng(img.bytes());

                    if (thumb == null) {
                        log.warn("[LinkCreate] thumbnail resize failed.");
                    }

                    if (thumb != null) {
                        // 4️⃣ 썸네일 S3 key 생성
                        String key = keyGenerator.generateThumbPng();

                        // 5️⃣ S3 업로드
                        thumbnailKey = s3Uploader.uploadPrivate(
                                key,
                                thumb.bytes(),
                                thumb.contentType()
                        );

                        log.info("[LinkCreate] thumbnail uploaded. key={}", thumbnailKey);

                        // 6️⃣ presigned URL 발급
                        thumbnailUrl =
                                presignedUrlProvider.presignGetUrl(thumbnailKey);
                    }
                }
            }

            // 7️⃣ 결과 반환
            return new LinkCreateResult(
                    og.title(),
                    og.description(),
                    og.siteName(),
                    og.canonicalUrl(),
                    thumbnailKey,
                    thumbnailUrl
            );

        } catch (Exception e) {
            log.error("[LinkCreate] failed. url={}", url, e);
            throw e;
        }
    }
}