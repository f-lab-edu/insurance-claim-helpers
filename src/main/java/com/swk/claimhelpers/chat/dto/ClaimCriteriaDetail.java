package com.swk.claimhelpers.chat.dto;

import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;

public record ClaimCriteriaDetail(
        Long id,
        String fileName,
        ClaimCriteriaStatus status
) {
}