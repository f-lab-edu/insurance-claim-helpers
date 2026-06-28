package com.swk.claimhelpers.chat.dto;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.MessageRole;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}