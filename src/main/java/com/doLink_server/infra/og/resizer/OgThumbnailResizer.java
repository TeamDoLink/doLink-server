package com.doLink_server.infra.og.resizer;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * OG 이미지 썸네일 생성 컴포넌트.
 *
 * - 원본 이미지를 정사각형 cover crop 후
 *   256x256 PNG 썸네일로 변환한다.
 * - 안정성과 호환성을 최우선으로 한다.
 */
@Component
public class OgThumbnailResizer {

    private static final int SIZE = 256;

    /**
     * @param originalBytes 원본 이미지 bytes
     * @return PNG 썸네일 이미지 (실패 시 null)
     */
    public ResizedImage toSquarePng(byte[] originalBytes) {
        try (
                ByteArrayInputStream in = new ByteArrayInputStream(originalBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            Thumbnails.of(in)
                    .size(SIZE, SIZE)
                    .crop(Positions.CENTER)
                    .outputFormat("png")
                    .outputQuality(0.9)
                    .toOutputStream(out);

            return new ResizedImage(out.toByteArray(), "image/png");
        } catch (Exception e) {
            return null;
        }
    }

    public record ResizedImage(byte[] bytes, String contentType) {}
}
