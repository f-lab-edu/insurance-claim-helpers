package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatContext;
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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock QueryRewriter queryRewriter;

    @InjectMocks ChatMessageService service;

    @Captor ArgumentCaptor<List<ChatMessage>> historyCaptor;

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

    // ── loadContext ──────────────────────────────────────────

    @Test
    void 연결_약관_없으면_예외() {
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.loadContext(1L, "질문", null, "sk"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_CLAIM_CRITERIA_ATTACHED);
    }

    @Test
    void USER_메시지_저장_후_약관과_최근이력_컨텍스트_반환() {
        ChatSessionClaimCriteria link = linkWithCriteriaId(10L);
        when(chatSessionService.findOwned(1L, null, "sk")).thenReturn(mock(ChatSession.class));
        when(linkRepository.findByChatSessionId(1L)).thenReturn(List.of(link));
        ChatMessage saved = mock(ChatMessage.class);
        when(chatMessageRepository.findByChatSessionIdOrderByIdDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(saved));

        ChatContext context = service.loadContext(1L, "질문", null, "sk");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(MessageRole.USER);
        assertThat(captor.getValue().getContent()).isEqualTo("질문");
        assertThat(context.criteriaIds()).containsExactly(10L);
        assertThat(context.recentHistory()).containsExactly(saved);
    }

    // ── prepareContext ───────────────────────────────────────

    @Test
    void 검색_요청에_재작성_쿼리와_IN_필터_임계값_구성() {
        when(queryRewriter.rewrite(eq("질문"), anyList())).thenReturn("재작성된 질문");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatContext context = new ChatContext(List.of(10L, 20L), List.of(mock(ChatMessage.class)));

        service.prepareContext("질문", context);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("재작성된 질문");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
        Filter.Expression expr = request.getFilterExpression();
        assertThat(expr.type()).isEqualTo(Filter.ExpressionType.IN);
        assertThat(((Filter.Key) expr.left()).key()).isEqualTo("claim_criteria_id");
    }

    @Test
    void 후속질문은_현재질문_제외한_이력으로_재작성() {
        ChatMessage q1 = mock(ChatMessage.class);
        ChatMessage a1 = mock(ChatMessage.class);
        ChatMessage current = mock(ChatMessage.class);   // 현재 질문
        when(queryRewriter.rewrite(eq("질문"), anyList())).thenReturn("재작성");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatContext context = new ChatContext(List.of(10L), List.of(q1, a1, current));

        service.prepareContext("질문", context);

        verify(queryRewriter).rewrite(eq("질문"), historyCaptor.capture());
        assertThat(historyCaptor.getValue()).containsExactly(q1, a1);   // current(현재 질문) 제외
    }

    @Test
    void 관련_청크_있으면_프롬프트_빌더에_위임() {
        when(queryRewriter.rewrite(anyString(), anyList())).thenReturn("재작성");
        List<Document> chunks = List.of(new Document("청크"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(chunks);
        List<ChatMessage> recentHistory = List.of(mock(ChatMessage.class));
        List<Message> built = List.of();
        when(chatPromptBuilder.build(chunks, recentHistory)).thenReturn(built);
        ChatContext context = new ChatContext(List.of(10L), recentHistory);

        PreparedChat result = service.prepareContext("질문", context);

        assertThat(result.hasRelevantChunks()).isTrue();
        assertThat(result.messages()).isSameAs(built);
        verify(chatPromptBuilder).build(chunks, recentHistory);   // 프롬프트엔 현재 질문 포함한 전체 이력
    }

    @Test
    void 관련_청크_없으면_빈_프롬프트와_false_플래그() {
        when(queryRewriter.rewrite(anyString(), anyList())).thenReturn("재작성");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatContext context = new ChatContext(List.of(10L), List.of(mock(ChatMessage.class)));

        PreparedChat result = service.prepareContext("질문", context);

        assertThat(result.hasRelevantChunks()).isFalse();
        assertThat(result.messages()).isEmpty();
        verify(chatPromptBuilder, never()).build(any(), any());
    }

    // ── saveAssistant ────────────────────────────────────────

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