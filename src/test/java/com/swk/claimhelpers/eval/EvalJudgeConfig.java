package com.swk.claimhelpers.eval;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@TestConfiguration
public class EvalJudgeConfig {

    private static final String JUDGE_MODEL = "claude-opus-4-8";
    
    @Bean
    public ChatClient.Builder judgeChatClientBuilder(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(JUDGE_MODEL)
                        .temperature(0.0)
                        .build());
    }

    @Bean
    public FactCheckingEvaluator factCheckingEvaluator(ChatClient.Builder judgeChatClientBuilder) {
        return new FactCheckingEvaluator(judgeChatClientBuilder);
    }

    @Bean
    public RelevancyEvaluator relevancyEvaluator(ChatClient.Builder judgeChatClientBuilder) {
        return new RelevancyEvaluator(judgeChatClientBuilder);
    }

    @Bean
    public ContextRelevanceJudge contextRelevanceJudge(ChatClient.Builder judgeChatClientBuilder,
                                                       ResourceLoader resourceLoader) {
        Resource prompt = resourceLoader.getResource("classpath:/prompts/eval-context-relevance.st");
        return new ContextRelevanceJudge(judgeChatClientBuilder.build(), prompt);
    }

    @Bean
    public NegativeRejectionJudge negativeRejectionJudge(ChatClient.Builder judgeChatClientBuilder,
                                                         ResourceLoader resourceLoader) {
        Resource prompt = resourceLoader.getResource("classpath:/prompts/eval-negative-rejection.st");
        return new NegativeRejectionJudge(judgeChatClientBuilder.build(), prompt);
    }
}