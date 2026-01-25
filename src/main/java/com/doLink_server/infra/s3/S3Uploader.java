package com.doLink_server.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/**
 * S3Uploader
 *
 * 썸네일 이미지를 private S3 버킷에 업로드한다.
 * - Presigned 방식에서는 DB에 URL이 아니라 "S3 key"를 저장한다.
 */
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;

    @Value("${app.s3.bucket}")
    private String bucket;

    /**
     * @param key         S3 객체 key (예: og-thumbnail/xxxx.jpg)
     * @param bytes       이미지 바이트
     * @param contentType 이미지 MIME 타입 (예: image/jpeg)
     * @return 저장된 S3 key (URL 아님)
     */
    public String uploadPrivate(String key, byte[] bytes, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256) // SSE-S3
                .build();

        s3.putObject(putReq, RequestBody.fromBytes(bytes));
        return key;
    }

}
