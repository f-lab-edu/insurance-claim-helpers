package com.swk.claimhelpers.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 떠 있는 LocalStack(localhost:4566)에 연결하는 통합 테스트.
 * 실행 전 docker-compose up -d 로 LocalStack/PostgreSQL 기동 필요.
 */
@SpringBootTest
class S3FileStorageTest {

    @Autowired
    private S3FileStorage s3FileStorage;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Test
    @DisplayName("upload 한 객체를 S3 에서 읽을 수 있고, delete 하면 사라진다")
    void upload_then_delete() {
        // given: 충돌 없는 임의 키와 바이트
        String objectKey = "test/" + UUID.randomUUID() + ".pdf";
        byte[] content = "dummy-pdf-content".getBytes(StandardCharsets.UTF_8);

        // when: 업로드
        s3FileStorage.upload(objectKey, content, "application/pdf");

        // then: S3 에서 동일 내용으로 조회됨
        ResponseBytes<GetObjectResponse> stored = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
        assertThat(stored.asByteArray()).isEqualTo(content);

        // when: 삭제
        s3FileStorage.delete(objectKey);

        // then: 더 이상 존재하지 않음 (headObject 시 NoSuchKey)
        assertThatThrownBy(() -> s3Client.headObject(
                HeadObjectRequest.builder().bucket(bucket).key(objectKey).build()))
                .isInstanceOf(NoSuchKeyException.class);
    }
}