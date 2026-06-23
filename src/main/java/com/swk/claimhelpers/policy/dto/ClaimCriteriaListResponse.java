package com.swk.claimhelpers.policy.dto;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;

import java.time.LocalDateTime;

public record ClaimCriteriaListResponse(
        Long id,
        String fileName,
        Long fileSize,
        ClaimCriteriaStatus status,
        LocalDateTime createdAt
) {
    public static ClaimCriteriaListResponse from(Document document) {
        ClaimCriteria claimCriteria = document.getClaimCriteria();
        return new ClaimCriteriaListResponse(
                claimCriteria.getId(),
                document.getFileName(),
                document.getFileSize(),
                claimCriteria.getStatus(),
                claimCriteria.getCreatedAt()
        );
    }
}