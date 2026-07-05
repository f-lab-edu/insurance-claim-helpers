package com.swk.claimhelpers.chat.dto;

import java.util.List;

public record ChatSessionDetailResponse(
        Long id,
        List<ClaimCriteriaDetail> claimCriteria,
        List<ChatMessageResponse> messages
) {
}