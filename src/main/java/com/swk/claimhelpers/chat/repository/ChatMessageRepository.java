package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(UUID chatSessionId);
}