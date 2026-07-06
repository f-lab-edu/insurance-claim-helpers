package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.MessageRole;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPromptBuilderTest {

    private ChatPromptBuilder newBuilder() {
        ChatPromptBuilder builder = new ChatPromptBuilder();
        ReflectionTestUtils.setField(builder, "systemPromptResource",
                new ClassPathResource("prompts/system.st"));
        return builder;
    }

    @Test
    void 시스템_메시지에_청크_컨텍스트_주입() {
        List<Document> chunks = List.of(new Document("3조 보장내용"), new Document("4조 면책"));

        List<Message> messages = newBuilder().build(chunks, List.of());

        Message system = messages.get(0);
        assertThat(system.getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(system.getText()).contains("3조 보장내용").contains("4조 면책");
    }

    @Test
    void 이력을_역할_순서대로_매핑() {
        ChatMessage user = ChatMessage.create(null, MessageRole.USER, "질문");
        ChatMessage assistant = ChatMessage.create(null, MessageRole.ASSISTANT, "답변");

        List<Message> messages = newBuilder().build(List.of(), List.of(user, assistant));

        // index 0 은 system, 1=USER, 2=ASSISTANT
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("질문");
        assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(2).getText()).isEqualTo("답변");
    }
}