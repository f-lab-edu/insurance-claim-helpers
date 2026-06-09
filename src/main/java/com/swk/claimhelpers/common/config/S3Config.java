package com.swk.claimhelpers.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * AWS S3 클라이언트 설정.
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                // LocalStack 엔드포인트로 강제 지정. 실제 AWS 전환 시 이 줄 제거
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                // 정적 자격 증명. LocalStack 은 test/test 같은 임의 값을 허용
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                // path-style 접근(http://endpoint/bucket/key). LocalStack 은 virtual-host 방식을
                // 지원하지 않으므로 필수
                .forcePathStyle(true)
                .build();
    }
}