package com.doLink_server.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * S3PresignedUrlProvider
 *
 * private S3 객체에 접근할 수 있는 "임시 GET URL"을 발급한다.
 * - 프론트는 이 URL로 이미지 로딩 (<img src="...">)
 * - 만료되면 API 재조회 시 새 URL을 받는다.
 */
@Component
@RequiredArgsConstructor
public class S3PresignedUrlProvider {

    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.presign-expire-minutes:60}") // 만료시간 60분
    private long expireMinutes;

    /**
     * @param key S3 객체 key
     * @return presigned GET URL (문자열)
     */
    public String presignGetUrl(String key) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expireMinutes))
                .getObjectRequest(getReq)
                .build();

        return presigner.presignGetObject(presignReq).url().toString();
    }
}
