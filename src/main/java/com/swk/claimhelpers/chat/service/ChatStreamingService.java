package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatStreamDelta;
import com.swk.claimhelpers.chat.dto.ChatStreamDone;
import com.swk.claimhelpers.chat.dto.PreparedChat;
import com.swk.claimhelpers.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamingService {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private static final String NO_RELEVANT_CRITERIA_MESSAGE =
            "관련된 약관 조항을 찾지 못해 답변드리기 어렵습니다. 첨부하신 약관을 확인하시거나 질문을 더 구체적으로 작성해 주세요.";

    private final ChatMessageService chatMessageService;
    private final ChatClient chatClient;

    public SseEmitter stream(Long sessionId, String content, User user, String sessionKey) {
        PreparedChat prepared = chatMessageService.prepare(sessionId, content, user, sessionKey);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        if(!prepared.hasRelevantChunks()) {
            respondFixed(emitter, sessionId, NO_RELEVANT_CRITERIA_MESSAGE);
            return emitter;
        }

        StringBuilder responseBuffer = new StringBuilder();
        chatClient.prompt()
                .messages(prepared.messages())
                .stream()
                .content()
                .subscribe(
                        delta -> sendDelta(emitter, responseBuffer, delta),
                        error -> {
                            log.error("채팅 스트림 실패: sessionId={}", sessionId, error);
                            emitter.completeWithError(error);
                        },
                        () -> complete(emitter, sessionId, responseBuffer.toString()));

        return emitter;
    }

    private void respondFixed(SseEmitter emitter, Long sessionId, String message) {
        StringBuilder responseBuffer = new StringBuilder();
        sendDelta(emitter, responseBuffer, message);
        complete(emitter, sessionId, responseBuffer.toString());
    }

    private void sendDelta(SseEmitter emitter, StringBuilder responseBuffer, String delta) {
        responseBuffer.append(delta);
        try {
            emitter.send(SseEmitter.event().data(new ChatStreamDelta(delta)));
        } catch(IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void complete(SseEmitter emitter, Long sessionId, String fullContent) {
        try {
            Long messageId = chatMessageService.saveAssistant(sessionId, fullContent);
            emitter.send(SseEmitter.event().data(new ChatStreamDone(true, messageId)));
            emitter.complete();
        } catch(IOException e) {
            emitter.completeWithError(e);
        }
    }
}