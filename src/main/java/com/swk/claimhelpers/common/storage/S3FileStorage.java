package com.swk.claimhelpers.common.storage;

import com.swk.claimhelpers.common.config.AwsS3Properties;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

/**
 * 약관 PDF 를 S3(개발 환경 LocalStack)에 저장/삭제하는 순수 스토리지 모듈.
 */
@Slf4j
@Component
public class S3FileStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, AwsS3Properties props) {
        this.s3Client = s3Client;
        this.bucket = props.bucket();
    }

    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            // SDK 가 미리 길이를 알아야 Content-Length 를 세팅하므로 contentLength 를 함께 받는다.
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (SdkException e) {
            log.error("S3 업로드 실패: key={}", objectKey, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    /**
     * 오브젝트를 통째로 읽어 byte[] 로 반환한다.
     *
     * PDFBox 가 PDF 를 random access 로 읽으므로
     * byte[]로 반환해야 한다.
     */
    public byte[] download(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (SdkException e) {
            log.error("S3 다운로드 실패: key={}", objectKey, e);
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED, e);
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