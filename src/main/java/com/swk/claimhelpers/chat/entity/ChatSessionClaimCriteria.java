package com.swk.claimhelpers.chat.entity;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_session_claim_criteria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSessionClaimCriteria {

    @EmbeddedId
    private ChatSessionClaimCriteriaId id;

    @MapsId("chatSessionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @MapsId("claimCriteriaId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_criteria_id", nullable = false)
    private ClaimCriteria claimCriteria;

    public static ChatSessionClaimCriteria create(ChatSession chatSession, ClaimCriteria claimCriteria) {
        ChatSessionClaimCriteria cscc = new ChatSessionClaimCriteria();
        cscc.id = ChatSessionClaimCriteriaId.of(chatSession.getId(), claimCriteria.getId());
        cscc.chatSession = chatSession;
        cscc.claimCriteria = claimCriteria;
        return cscc;
    }
}