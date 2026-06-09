package com.swk.claimhelpers.common.storage;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 약관 PDF 를 S3(개발 환경 LocalStack)에 저장/삭제하는 순수 스토리지 모듈.
 */
@Slf4j
@Component
public class S3FileStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }
    
    public void upload(String objectKey, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException e) {
            log.error("S3 업로드 실패: key={}", objectKey, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    /**
     * claim_criteria 삭제 시 #15 에서 호출한다.
     */
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            log.error("S3 삭제 실패: key={}", objectKey, e);
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED, e);
        }
    }
}