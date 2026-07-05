package com.swk.claimhelpers.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSessionCreateResponse(
        Long id,
        List<ClaimCriteriaSummary> claimCriteria,
        LocalDateTime createdAt
) {
}