package com.swk.claimhelpers.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class ChatSessionPolicyId implements Serializable {

    @Column(name = "chat_session_id", nullable = false)
    private UUID chatSessionId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    public static ChatSessionPolicyId of(UUID chatSessionId, UUID policyId) {
        ChatSessionPolicyId id = new ChatSessionPolicyId();
        id.chatSessionId = chatSessionId;
        id.policyId = policyId;
        return id;
    }
}