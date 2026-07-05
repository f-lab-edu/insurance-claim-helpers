package com.swk.claimhelpers.chat.dto;

import com.swk.claimhelpers.chat.entity.ChatMessage;

import java.util.List;

public record ChatContext(List<Long> criteriaIds, List<ChatMessage> recentHistory) {
}