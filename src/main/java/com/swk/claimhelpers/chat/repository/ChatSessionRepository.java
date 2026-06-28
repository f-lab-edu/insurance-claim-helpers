package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserId(Long userId);

    Optional<ChatSession> findBySessionKey(String sessionKey);

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}