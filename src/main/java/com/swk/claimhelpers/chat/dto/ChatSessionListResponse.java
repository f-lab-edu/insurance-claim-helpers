package com.swk.claimhelpers.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSessionListResponse(
        Long id,
        List<ClaimCriteriaSummary> claimCriteria,
        String lastMessage,
        LocalDateTime createdAt
) {
}