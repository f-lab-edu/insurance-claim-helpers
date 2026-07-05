package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.MessageRole;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QueryRewriterTest {

    private ChatMessage message(MessageRole role, String content) {
        ChatMessage message = mock(ChatMessage.class);
        when(message.getRole()).thenReturn(role);
        when(message.getContent()).thenReturn(content);
        return message;
    }

    @Test
    void 이력_없으면_원_질문_그대로_반환하고_LLM_미호출() {
        ChatClient chatClient = mock(ChatClient.class);
        QueryRewriter rewriter = new QueryRewriter(chatClient);

        String result = rewriter.rewrite("암 진단비 청구 가능한가요?", List.of());

        assertThat(result).isEqualTo("암 진단비 청구 가능한가요?");
        verifyNoInteractions(chatClient);
    }

    @Test
    void 이력_있으면_재작성_결과를_반환() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("1조 2항의 내용은 무엇인가요?");
        QueryRewriter rewriter = new QueryRewriter(chatClient);
        ReflectionTestUtils.setField(rewriter, "promptResource",
                new ClassPathResource("prompts/query-rewrite.st"));

        List<ChatMessage> history = List.of(
                message(MessageRole.USER, "1조 1항이 뭐야?"),
                message(MessageRole.ASSISTANT, "1조 1항은 ...입니다."));

        String result = rewriter.rewrite("그 다음 항은?", history);

        assertThat(result).isEqualTo("1조 2항의 내용은 무엇인가요?");
    }
}