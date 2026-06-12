package com.swk.claimhelpers.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
        String endpoint,    // local: LocalStack, prod: OCI S3 호환 엔드포인트
        String region,
        String bucket,
        String accessKey,   // local: test, prod: OCI Customer Secret Key
        String secretKey
) {
}