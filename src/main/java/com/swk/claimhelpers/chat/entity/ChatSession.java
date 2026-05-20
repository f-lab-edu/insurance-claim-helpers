package com.swk.claimhelpers.chat.entity;

import com.swk.claimhelpers.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // nullable — 비로그인 사용자는 user_id 없음
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_key", length = 255)
    private String sessionKey;

    @OneToMany(mappedBy = "chatSession")
    private List<ChatSessionPolicy> sessionPolicies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ChatSession createForUser(User user) {
        ChatSession session = new ChatSession();
        session.user = user;
        return session;
    }

    public static ChatSession createForSession(String sessionKey) {
        ChatSession session = new ChatSession();
        session.sessionKey = sessionKey;
        return session;
    }
}