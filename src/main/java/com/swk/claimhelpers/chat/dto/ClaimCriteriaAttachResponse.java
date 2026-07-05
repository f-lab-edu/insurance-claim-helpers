package com.swk.claimhelpers.chat.dto;

public record ClaimCriteriaAttachResponse(
        Long chatSessionId,
        Long claimCriteriaId,
        String fileName
) {
}