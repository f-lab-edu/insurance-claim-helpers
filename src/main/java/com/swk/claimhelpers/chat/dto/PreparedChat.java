package com.swk.claimhelpers.chat.dto;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 스트림 시작 전 동기 준비 결과.
 *
 * @param messages          Claude 로 보낼 메시지 목록(system + 대화 이력). hasRelevantChunks=false 면 빈 목록.
 * @param hasRelevantChunks 유사도 임계값을 통과한 관련 약관 청크가 있었는지 여부.
 *                          false 면 LLM 호출 없이 고정 안내 답변으로 처리
 */
public record PreparedChat(List<Message> messages, boolean hasRelevantChunks) {
}