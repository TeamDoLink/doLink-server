package com.doLink_server.infra.og.resizer;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * OG 이미지 썸네일 생성 컴포넌트.
 *
 * - 원본 이미지를 정사각형 cover crop 후
 *   256x256 PNG 썸네일로 변환한다.
 * - 실패 시 null 반환 (OG 흐름 차단 방지)
 */
@Component
public class OgThumbnailResizer {

    private static final int SIZE = 256;

    /** 이미지 폭탄 방지를 위한 입력 제한 */
    private static final int MAX_ORIGINAL_BYTES = 2 * 1024 * 1024;

    /**
     * @param originalBytes 원본 이미지 bytes
     * @return PNG 썸네일 이미지 (실패 시 null)
     */
    public ResizedImage toSquarePng(byte[] originalBytes) {
        try (
                ByteArrayInputStream in = new ByteArrayInputStream(originalBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            // 이미지 여부 확인 (HTML/PDF 등 차단)
            BufferedImage image = ImageIO.read(in);
            if (image == null || originalBytes.length > MAX_ORIGINAL_BYTES) {
                return null;
            }

            Thumbnails.of(image)
                    .size(SIZE, SIZE)
                    .crop(Positions.CENTER)        // 중앙 기준 cover crop
                    .useExifOrientation(true)      // 모바일 이미지 회전 보정
                    .outputFormat("png")           // OG 호환성 최우선
                    .outputQuality(0.9)
                    .toOutputStream(out);

            return new ResizedImage(out.toByteArray(), "image/png");
        } catch (Exception e) {
            return null;
        }
    }

    public record ResizedImage(byte[] bytes, String contentType) {}
}
