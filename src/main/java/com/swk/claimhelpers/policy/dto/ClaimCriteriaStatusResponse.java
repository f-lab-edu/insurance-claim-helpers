package com.swk.claimhelpers.policy.dto;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;

public record ClaimCriteriaStatusResponse(
        Long id,
        ClaimCriteriaStatus status
) {
    public static ClaimCriteriaStatusResponse from(ClaimCriteria claimCriteria) {
        return new ClaimCriteriaStatusResponse(
                claimCriteria.getId(),
                claimCriteria.getStatus()
        );
    }
}