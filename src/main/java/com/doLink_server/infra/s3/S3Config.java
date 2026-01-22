package com.doLink_server.infra.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3Config
 *
 * - private,Presigned URL 발급용 S3Presigner Bean 등록
 * - S3 업로드용 S3Client
 *
 * 자격 증명은 기본 Credential Provider Chain을 사용:
 * (로컬: ~/.aws, 배포: IAM Role/Task Role 등)
 */
@Configuration
public class S3Config {

    @Value("${app.aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

}
