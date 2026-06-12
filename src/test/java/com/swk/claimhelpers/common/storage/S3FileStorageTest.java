package com.swk.claimhelpers.common.storage;

import com.swk.claimhelpers.common.config.AwsS3Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Spring 컨텍스트 없이 직접 생성(OAuth/DB 의존 제거).
 */
@Testcontainers
class S3FileStorageTest {

    // static @Container: 클래스의 모든 테스트가 공유하는 단일 컨테이너 (@BeforeAll 전에 기동, 전체 종료 후 정리)
    @Container
    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
                    .withServices(S3);

    static final String BUCKET = "test-bucket";

    static S3Client s3Client;
    static S3FileStorage s3FileStorage;

    @BeforeAll
    static void setUp() {
        // 컨테이너가 동적으로 띄운 엔드포인트/자격증명으로 클라이언트 구성
        s3Client = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .forcePathStyle(true)
                .build();
        s3Client.createBucket(b -> b.bucket(BUCKET));

        // S3FileStorage 가 실제로 쓰는 값은 bucket 뿐. 나머지 필드는 클라이언트 구성에만 쓰이므로 의미 없음
        AwsS3Properties props = new AwsS3Properties(
                LOCALSTACK.getEndpoint().toString(), LOCALSTACK.getRegion(), BUCKET,
                LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey());
        s3FileStorage = new S3FileStorage(s3Client, props);
    }

    @Test
    @DisplayName("upload 한 객체를 S3 에서 읽을 수 있고, delete 하면 사라진다")
    void upload_then_delete() {
        // given: 충돌 없는 임의 키와 바이트
        String objectKey = "test/" + UUID.randomUUID() + ".pdf";
        byte[] content = "dummy-pdf-content".getBytes(StandardCharsets.UTF_8);

        // when: 업로드 (InputStream + 길이)
        s3FileStorage.upload(objectKey, new ByteArrayInputStream(content), content.length, "application/pdf");

        // then: S3 에서 동일 내용으로 조회됨
        ResponseBytes<GetObjectResponse> stored = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(objectKey).build());
        assertThat(stored.asByteArray()).isEqualTo(content);

        // when: 삭제
        s3FileStorage.delete(objectKey);

        // then: 더 이상 존재하지 않음 (headObject 시 NoSuchKey)
        assertThatThrownBy(() -> s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(objectKey).build()))
                .isInstanceOf(NoSuchKeyException.class);
    }
}