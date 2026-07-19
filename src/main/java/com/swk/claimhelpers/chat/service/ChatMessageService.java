package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatContext;
import com.swk.claimhelpers.chat.dto.PreparedChat;
import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.ChatSession;
import com.swk.claimhelpers.chat.entity.MessageRole;
import com.swk.claimhelpers.chat.repository.ChatMessageRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionClaimCriteriaRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionRepository;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.policy.service.ClaimCriteriaVectorMetadata;
import com.swk.claimhelpers.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int TOP_K = 5;

    private static final int RECENT_MESSAGE_COUNT = 10;

    private final ChatSessionService chatSessionService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionClaimCriteriaRepository chatSessionClaimCriteriaRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VectorStore vectorStore;
    private final ChatPromptBuilder chatPromptBuilder;
    private final QueryRewriter queryRewriter;

    @Value("${chat.rag.similarity-threshold}")
    private double similarityThreshold;
    
    @Transactional
    public ChatContext loadContext(Long sessionId, String content, User user, String sessionKey) {

        ChatSession session = chatSessionService.findOwned(sessionId, user, sessionKey);

        List<Long> criteriaIds = chatSessionClaimCriteriaRepository.findByChatSessionId(sessionId).stream()
                .map(link -> link.getClaimCriteria().getId())
                .toList();
        if(criteriaIds.isEmpty()) {
            throw new CustomException(ErrorCode.NO_CLAIM_CRITERIA_ATTACHED);
        }

        chatMessageRepository.save(ChatMessage.create(session, MessageRole.USER, content));

        List<ChatMessage> recentHistory = new ArrayList<>(
                chatMessageRepository.findByChatSessionIdOrderByIdDesc(
                        sessionId, PageRequest.of(0, RECENT_MESSAGE_COUNT)));
        Collections.reverse(recentHistory);

        return new ChatContext(criteriaIds, recentHistory);
    }
    
    public PreparedChat prepareContext(String content, ChatContext context) {
        List<Document> chunks = retrieve(content, context);
        if(chunks.isEmpty()) {
            return new PreparedChat(List.of(), false);
        }
        return new PreparedChat(chatPromptBuilder.build(chunks, context.recentHistory()), true);
    }
    
    public List<Document> retrieve(String content, ChatContext context) {
        List<ChatMessage> recentHistory = context.recentHistory();
        List<ChatMessage> priorHistory = recentHistory.subList(0, recentHistory.size() - 1);
        String searchQuery = queryRewriter.rewrite(content, priorHistory);
        return searchChunks(searchQuery, context.criteriaIds());
    }
    
    @Transactional
    public Long saveAssistant(Long sessionId, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.create(session, MessageRole.ASSISTANT, content));
        return saved.getId();
    }

    private List<Document> searchChunks(String query, List<Long> criteriaIds) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .in(ClaimCriteriaVectorMetadata.CLAIM_CRITERIA_ID, criteriaIds.toArray())
                .build();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(TOP_K)
                .similarityThreshold(similarityThreshold)
                .filterExpression(filter)
                .build();
        return vectorStore.similaritySearch(request);
    }
}