package com.swk.claimhelpers.chat.entity;

import com.swk.claimhelpers.policy.entity.Policy;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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