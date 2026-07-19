package com.swk.claimhelpers.eval;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

/**
 * Negative Rejection(기권) 평가.
 * 함정질문(약관에 없는 내용)에 대해 AI 가 단정하지 않고 기권했는지 판정한다.
 */
public class NegativeRejectionJudge {

    private final ChatClient judgeChatClient;
    private final Resource promptResource;

    public NegativeRejectionJudge(ChatClient judgeChatClient, Resource promptResource) {
        this.judgeChatClient = judgeChatClient;
        this.promptResource = promptResource;
    }

    public boolean evaluate(String question, String answer) {
        String raw = judgeChatClient.prompt()
                .user(spec -> spec.text(promptResource)
                        .param("question", question)
                        .param("answer", answer == null ? "" : answer))
                .call()
                .content();
        return parseAbstained(raw);
    }

    boolean parseAbstained(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("YES");
    }
}