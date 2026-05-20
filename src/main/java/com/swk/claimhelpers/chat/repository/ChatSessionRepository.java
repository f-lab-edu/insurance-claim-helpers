package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserId(UUID userId);

    Optional<ChatSession> findBySessionKey(String sessionKey);
}