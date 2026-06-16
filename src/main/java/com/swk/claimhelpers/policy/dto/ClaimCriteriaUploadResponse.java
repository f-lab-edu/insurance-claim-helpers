package com.swk.claimhelpers.policy.dto;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;

import java.time.LocalDateTime;

/**
 * 약관 PDF 업로드 응답.
 *
 * id 는 claim_criteria.id — 상태 폴링·채팅 연결 등 후속 API 가 모두 이 값을 키로 사용한다.
 * fileName/fileSize 는 documents 에, status/createdAt 은 claim_criteria 에 있어 두 엔티티를 함께 받는다.
 */
public record ClaimCriteriaUploadResponse(
        Long id,
        String fileName,
        Long fileSize,
        ClaimCriteriaStatus status,
        LocalDateTime createdAt
) {
    public static ClaimCriteriaUploadResponse from(ClaimCriteria claimCriteria, Document document) {
        return new ClaimCriteriaUploadResponse(
                claimCriteria.getId(),
                document.getFileName(),
                document.getFileSize(),
                claimCriteria.getStatus(),
                claimCriteria.getCreatedAt()
        );
    }
}