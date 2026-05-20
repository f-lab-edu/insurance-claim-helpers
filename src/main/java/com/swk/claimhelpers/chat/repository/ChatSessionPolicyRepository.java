package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSessionPolicy;
import com.swk.claimhelpers.chat.entity.ChatSessionPolicyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ChatSessionPolicyRepository extends JpaRepository<ChatSessionPolicy, ChatSessionPolicyId> {

    List<ChatSessionPolicy> findByChatSessionId(UUID chatSessionId);
}