package com.swk.claimhelpers.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class ChatSessionClaimCriteriaId implements Serializable {

    @Column(name = "chat_session_id", nullable = false)
    private Long chatSessionId;

    @Column(name = "claim_criteria_id", nullable = false)
    private Long claimCriteriaId;

    public static ChatSessionClaimCriteriaId of(Long chatSessionId, Long claimCriteriaId) {
        ChatSessionClaimCriteriaId id = new ChatSessionClaimCriteriaId();
        id.chatSessionId = chatSessionId;
        id.claimCriteriaId = claimCriteriaId;
        return id;
    }
}