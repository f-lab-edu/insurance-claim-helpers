package com.swk.claimhelpers.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * 오브젝트 스토리지(S3 호환) 클라이언트 설정.
 */
@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class S3Config {

    @Bean
    @Profile("local")
    public S3Client localS3Client(AwsS3Properties props) {
        return s3CompatibleClient(props);
    }

    // 운영: OCI Object Storage(S3 호환). 실제 연결값/검증은 OCI 전환 이슈에서 채운다
    @Bean
    @Profile("prod")
    public S3Client prodS3Client(AwsS3Properties props) {
        return s3CompatibleClient(props);
    }

    // LocalStack/OCI 공통 빌더: S3 호환 엔드포인트 + 정적 자격증명 + path-style
    private S3Client s3CompatibleClient(AwsS3Properties props) {
        return S3Client.builder()
                // S3 호환 엔드포인트로 강제 지정(LocalStack 또는 OCI 네임스페이스 엔드포인트)
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                // 정적 자격 증명. LocalStack=test/test, OCI=Customer Secret Key
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.accessKey(), props.secretKey())))
                // path-style 접근(http://endpoint/bucket/key). LocalStack 은 virtual-host 미지원,
                // OCI S3 호환도 path-style 권장
                .forcePathStyle(true)
                .build();
    }
}