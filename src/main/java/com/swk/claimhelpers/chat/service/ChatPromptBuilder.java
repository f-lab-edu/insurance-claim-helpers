package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.MessageRole;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChatPromptBuilder {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    /**
     * @param chunks  VectorStore 유사도 검색 결과 청크. 첫 SystemMessage 의 {context} 로 주입.
     * @param history 세션의 전체 대화 이력(시간순). 마지막이 현재 질문.
     * @return index 0 = SystemMessage, 이어서 history 를 순서대로 User/Assistant 메시지로 매핑한 목록
     */
    public List<Message> build(List<Document> chunks, List<ChatMessage> history) {
        
        String context = chunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        Message system = new SystemPromptTemplate(systemPromptResource)
                .createMessage(Map.of("context", context));

        List<Message> messages = new ArrayList<>();
        messages.add(system);
        
        for(ChatMessage message : history) {
            if(message.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(message.getContent()));
            } else {
                messages.add(new AssistantMessage(message.getContent()));
            }
        }
        return messages;
    }
}