package com.swk.claimhelpers.chat.entity;

import com.swk.claimhelpers.policy.entity.Policy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_session_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSessionPolicy {

    @EmbeddedId
    private ChatSessionPolicyId id;

    @MapsId("chatSessionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @MapsId("policyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    public static ChatSessionPolicy create(ChatSession chatSession, Policy policy) {
        ChatSessionPolicy csp = new ChatSessionPolicy();
        csp.id = ChatSessionPolicyId.of(chatSession.getId(), policy.getId());
        csp.chatSession = chatSession;
        csp.policy = policy;
        return csp;
    }
}