package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);

    @Query("SELECT m FROM ChatMessage m WHERE m.id IN "
            + "(SELECT MAX(m2.id) FROM ChatMessage m2 WHERE m2.chatSession.id IN :sessionIds GROUP BY m2.chatSession.id)")
    List<ChatMessage> findLastMessagesByChatSessionIds(@Param("sessionIds") Collection<Long> sessionIds);
}