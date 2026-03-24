package com.doLink_server.infra.og.parser;

import com.doLink_server.infra.og.dto.OgMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * - 외부 URL HTML을 조회하여 OG/Twitter/Title 메타데이터를 파싱
 * - 파싱 실패 시에도 링크 저장 흐름을 중단하지 않도록 null 기반 결과 반환
 */
@Component
public class OgMetadataParser {

    /** OG 파싱 시 네트워크 지연 방지를 위한 최대 대기 시간 */
    private static final int TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();

    /**
     * - URL 페이지의 OG 메타데이터를 파싱하여 OgMetadata로 반환
     * - 일부 메타데이터가 없을 경우 fallback 규칙을 적용
     * - 예외 발생 시 빈 OgMetadata(null 기반)를 반환
     */
    public OgMetadata parse(String url) {
        try {
            org.jsoup.Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            if (response.statusCode() >= 400) {
                return OgMetadata.invalid();
            }

            Document doc = response.parse();

            // OG 메타데이터 우선 추출
            String ogTitle = meta(doc, "property", "og:title");
            String ogDesc  = meta(doc, "property", "og:description");
            String ogImage = meta(doc, "property", "og:image");
            String ogSite  = meta(doc, "property", "og:site_name");
            String ogUrl   = meta(doc, "property", "og:url");

            // Twitter 메타데이터 fallback
            if (isBlank(ogTitle)) ogTitle = meta(doc, "name", "twitter:title");
            if (isBlank(ogDesc))  ogDesc  = meta(doc, "name", "twitter:description");
            if (isBlank(ogImage)) ogImage = meta(doc, "name", "twitter:image");

            // title 태그 fallback
            if (isBlank(ogTitle)) ogTitle = titleTag(doc);

            // canonical URL fallback
            String canonical = canonical(doc);
            String finalUrl = !isBlank(ogUrl) ? ogUrl : canonical;

            return new OgMetadata(
                    trimToNull(ogTitle),
                    trimToNull(ogDesc),
                    trimToNull(ogImage),
                    trimToNull(ogSite),
                    trimToNull(finalUrl),
                    true
            );

        } catch (Exception e) {
            return OgMetadata.invalid();
        }
    }

    /**
     * - meta 태그에서 특정 key(property/name)의 content 값을 추출
     * - 해당 meta 태그가 없을 경우 null 반환
     */
    private String meta(Document doc, String keyAttr, String keyValue) {
        Element el = doc.selectFirst("meta[" + keyAttr + "=" + keyValue + "]");
        return el != null ? el.attr("content") : null;
    }

    /**
     * - <title> 태그의 값을 추출
     * - title 태그가 없거나 비어 있으면 null 반환
     */
    private String titleTag(Document doc) {
        String t = doc.title();
        return (t != null && !t.isBlank()) ? t : null;
    }

    /**
     * - canonical link 태그에서 대표 URL을 추출
     * - canonical 태그가 없으면 null 반환
     */
    private String canonical(Document doc) {
        Element el = doc.selectFirst("link[rel=canonical]");
        return el != null ? el.attr("href") : null;
    }

    /**
     * - 문자열이 null 또는 공백인지 여부를 판단
     */
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * - 문자열의 앞뒤 공백을 제거
     * - 결과가 빈 문자열이면 null 반환
     */
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

}
