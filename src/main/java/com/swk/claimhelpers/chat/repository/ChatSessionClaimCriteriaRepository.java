package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteria;
import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteriaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ChatSessionClaimCriteriaRepository extends JpaRepository<ChatSessionClaimCriteria, ChatSessionClaimCriteriaId> {

    List<ChatSessionClaimCriteria> findByChatSessionId(Long chatSessionId);
}