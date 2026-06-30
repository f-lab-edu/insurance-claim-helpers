package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.PreparedChat;
import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.ChatSession;
import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteria;
import com.swk.claimhelpers.chat.entity.MessageRole;
import com.swk.claimhelpers.chat.repository.ChatMessageRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionClaimCriteriaRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionRepository;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock ChatSessionService chatSessionService;
    @Mock ChatSessionRepository chatSessionRepository;
    @Mock ChatSessionClaimCriteriaRepository linkRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock VectorStore vectorStore;
    @Mock ChatPromptBuilder chatPromptBuilder;

    @InjectMocks ChatMessageService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.5);
    }

    private ChatSessionClaimCriteria linkWithCriteriaId(long id) {
        ClaimCriteria criteria = mock(ClaimCriteria.class);
        when(criteria.getId()).thenReturn(id);
        ChatSessionClaimCriteria link = mock(ChatSessionClaimCriteria.class);
        when(link.getClaimCriteria()).thenReturn(criteria);
        return link;
    }

    @Test
    void 연결_약관_없으면_예외() {
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.prepare(1L, "질문", null, "sk"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_CLAIM_CRITERIA_ATTACHED);
    }

    @Test
    void USER_메시지_저장() {
        ChatSessionClaimCriteria link = linkWithCriteriaId(10L);
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of(link));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.prepare(1L, "질문", null, "sk");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(MessageRole.USER);
        assertThat(captor.getValue().getContent()).isEqualTo("질문");
    }

    @Test
    void 검색_요청에_IN_필터와_임계값_구성() {
        ChatSessionClaimCriteria link1 = linkWithCriteriaId(10L);
        ChatSessionClaimCriteria link2 = linkWithCriteriaId(20L);
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of(link1, link2));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.prepare(1L, "질문", null, "sk");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("질문");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
        Filter.Expression expr = request.getFilterExpression();
        assertThat(expr.type()).isEqualTo(Filter.ExpressionType.IN);
        assertThat(((Filter.Key) expr.left()).key()).isEqualTo("claim_criteria_id");
    }

    @Test
    void 관련_청크_있으면_프롬프트_빌더에_위임() {
        ChatSessionClaimCriteria link = linkWithCriteriaId(10L);
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of(link));
        List<Document> chunks = List.of(new Document("청크"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(chunks);
        List<ChatMessage> history = List.of();
        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(1L)).thenReturn(history);
        List<Message> built = List.of();
        when(chatPromptBuilder.build(chunks, history)).thenReturn(built);

        PreparedChat result = service.prepare(1L, "질문", null, "sk");

        assertThat(result.hasRelevantChunks()).isTrue();
        assertThat(result.messages()).isSameAs(built);
        verify(chatPromptBuilder).build(chunks, history);
    }

    @Test
    void 관련_청크_없으면_빈_프롬프트와_false_플래그() {
        ChatSessionClaimCriteria link = linkWithCriteriaId(10L);
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of(link));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        PreparedChat result = service.prepare(1L, "질문", null, "sk");

        assertThat(result.hasRelevantChunks()).isFalse();
        assertThat(result.messages()).isEmpty();
        verify(chatPromptBuilder, never()).build(any(), any());
    }

    @Test
    void ASSISTANT_저장_후_id_반환() {
        ChatSession session = mock(ChatSession.class);
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        ChatMessage saved = mock(ChatMessage.class);
        when(saved.getId()).thenReturn(99L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        Long id = service.saveAssistant(1L, "답변");

        assertThat(id).isEqualTo(99L);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(captor.getValue().getContent()).isEqualTo("답변");
    }
}