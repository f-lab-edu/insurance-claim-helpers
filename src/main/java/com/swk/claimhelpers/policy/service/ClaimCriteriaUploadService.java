package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ClaimCriteriaUploadService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    // 앱 검증 한도 = multipart 한도의 80%.
    private static final double APP_LIMIT_RATIO = 0.8;

    private final ClaimCriteriaRepository claimCriteriaRepository;
    private final DocumentRepository documentRepository;
    private final S3FileStorage s3FileStorage;
    private final MultipartProperties multipartProperties;

    /**
     * @param user       로그인 사용자(없으면 null)
     * @param sessionKey 비로그인 사용자 식별 키(로그인 시 null)
     * @return 저장된 Document (claimCriteria 참조 포함) — 컨트롤러가 응답·비동기 호출에 사용
     */
    @Transactional
    public Document upload(MultipartFile file, User user, String sessionKey) {
        validate(file);

        // 소유자에 따라 claim_criteria 생성 (CHECK 제약: user_id / session_key 중 하나만 존재)
        ClaimCriteria claimCriteria = (user != null)
                ? ClaimCriteria.createForUser(user)
                : ClaimCriteria.createForSession(sessionKey);
        claimCriteriaRepository.save(claimCriteria);

        Document document = Document.create(claimCriteria, file.getOriginalFilename(), file.getSize());
        documentRepository.save(document);

        String objectKey = "claim-criteria/" + claimCriteria.getId() + "/" + UUID.randomUUID() + ".pdf";
        s3FileStorage.upload(objectKey, openStream(file), file.getSize(), PDF_CONTENT_TYPE);

        document.updateObjectKey(objectKey);
        claimCriteria.updateStatus(ClaimCriteriaStatus.PROCESSING);

        return document;
    }

    private void validate(MultipartFile file) {
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        long maxBytes = maxFileSizeBytes();
        if (file.getSize() > maxBytes) {
            long maxMb = DataSize.ofBytes(maxBytes).toMegabytes();
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "파일 크기가 제한(" + maxMb + "MB)을 초과했습니다.");
        }
    }

    private long maxFileSizeBytes() {
        return (long) (multipartProperties.getMaxFileSize().toBytes() * APP_LIMIT_RATIO);
    }

    private InputStream openStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }
}