package com.swk.claimhelpers.eval;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public class ContextRelevanceJudge {

    private final ChatClient judgeChatClient;
    private final Resource promptResource;

    public ContextRelevanceJudge(ChatClient judgeChatClient, Resource promptResource) {
        this.judgeChatClient = judgeChatClient;
        this.promptResource = promptResource;
    }

    /**
     * 청크별 관련성 판정 후 관련 비율(precision)을 반환한다.
     * 빈 청크(검색 실패)면 관련 근거가 하나도 없으므로 0.0.
     */
    public double evaluate(String question, List<Document> chunks) {
        if(chunks.isEmpty()) {
            return 0.0;
        }
        List<Boolean> verdicts = chunks.stream()
                .map(chunk -> parseVerdict(judgeOne(question, chunk.getText())))
                .toList();
        return precision(verdicts);
    }

    private String judgeOne(String question, String chunkText) {
        return judgeChatClient.prompt()
                .user(spec -> spec.text(promptResource)
                        .param("question", question)
                        .param("chunk", chunkText == null ? "" : chunkText))
                .call()
                .content();
    }

    boolean parseVerdict(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("YES");
    }

    double precision(List<Boolean> verdicts) {
        if(verdicts.isEmpty()) {
            return 0.0;
        }
        long relevant = verdicts.stream().filter(Boolean::booleanValue).count();
        return (double) relevant / verdicts.size();
    }
}