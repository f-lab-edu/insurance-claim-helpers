package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QueryRewriter {

    @Value("classpath:/prompts/query-rewrite.st")
    private Resource promptResource;

    private final ChatClient queryRewriteChatClient;

    public QueryRewriter(@Qualifier("queryRewriteChatClient") ChatClient queryRewriteChatClient) {
        this.queryRewriteChatClient = queryRewriteChatClient;
    }

    /**
     * @param question 사용자의 마지막 질문
     * @param history  이전 대화 이력(현재 질문 제외, 시간순). 비어 있으면 재작성 불필요
     * @return 이력이 없으면 원 질문 그대로, 있으면 이력을 반영해 재작성한 독립 질문
     */
    public String rewrite(String question, List<ChatMessage> history) {
        if(history.isEmpty()) {
            return question;
        }

        String historyText = history.stream()
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = new PromptTemplate(promptResource)
                .render(Map.of("history", historyText, "question", question));

        return queryRewriteChatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}