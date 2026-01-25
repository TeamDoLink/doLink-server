package com.doLink_server.infra.og.downloader;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * - OG 썸네일(외부 URL)을 바이트로 다운로드
 * - 실패/예외 시 null 반환하여 링크 저장 흐름을 막지 않음
 */
@Component
public class ImageDownloader {

    /** 외부 이미지 요청에 사용되는 HTTP 클라이언트 */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * - 외부 이미지 URL을 GET으로 조회하여 bytes + contentType 반환
     * - 응답이 비정상(2xx 아님/본문 없음)이면 null 반환
     */
    public DownloadedImage download(String imageUrl) {
        try {
            ResponseEntity<byte[]> res = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    byte[].class
            );

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                return null;
            }

            MediaType ct = res.getHeaders().getContentType();
            String contentType = (ct != null) ? ct.toString() : "application/octet-stream";

            return new DownloadedImage(res.getBody(), contentType);

        } catch (Exception e) {
            // 다운로드 실패 시에도 링크 저장 로직을 유지하기 위해 null 반환
            return null;
        }
    }
    /** - 이미지 바이트와 MIME 타입을 함께 전달하기 위한 record */
    public record DownloadedImage(byte[] bytes, String contentType) {}
}
