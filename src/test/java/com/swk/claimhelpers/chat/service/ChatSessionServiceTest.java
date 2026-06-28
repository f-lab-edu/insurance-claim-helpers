package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatSessionCreateResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionDetailResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionListResponse;
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
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatSessionClaimCriteriaRepository chatSessionClaimCriteriaRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatSessionService chatSessionService;

    private User userWithId(long id, String email) {
        User user = User.create(email);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ChatSession sessionWithId(long id, ChatSession session) {
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private ClaimCriteria criteriaWithId(long id, ClaimCriteria criteria) {
        ReflectionTestUtils.setField(criteria, "id", id);
        return criteria;
    }

    @Test
    @DisplayName("로그인 사용자가 본인 세션을 조회하면 엔티티를 반환한다")
    void 로그인_본인_성공() {
        User owner = userWithId(1L, "owner@gmail.com");
        ChatSession session = sessionWithId(10L, ChatSession.createForUser(owner));
        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));

        ChatSession result = chatSessionService.findOwned(10L, owner, null);

        assertThat(result).isSameAs(session);
    }

    @Test
    @DisplayName("존재하지 않는 id 면 SESSION_NOT_FOUND 예외를 던진다")
    void 없는_id_NOT_FOUND() {
        User owner = userWithId(1L, "owner@gmail.com");
        given(chatSessionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatSessionService.findOwned(99L, owner, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 사용자가 다른 사용자의 세션을 조회하면 FORBIDDEN 예외를 던진다")
    void 다른_user_FORBIDDEN() {
        User owner = userWithId(1L, "owner@gmail.com");
        User other = userWithId(2L, "other@gmail.com");
        ChatSession session = sessionWithId(10L, ChatSession.createForUser(owner));
        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatSessionService.findOwned(10L, other, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("비로그인 사용자가 본인 세션을 조회하면 엔티티를 반환한다")
    void 비로그인_본인_성공() {
        ChatSession session = sessionWithId(10L, ChatSession.createForSession("session-1"));
        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));

        ChatSession result = chatSessionService.findOwned(10L, null, "session-1");

        assertThat(result).isSameAs(session);
    }

    @Test
    @DisplayName("비로그인 사용자가 다른 세션 키로 조회하면 FORBIDDEN 예외를 던진다")
    void 다른_session_key_FORBIDDEN() {
        ChatSession session = sessionWithId(10L, ChatSession.createForSession("session-1"));
        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatSessionService.findOwned(10L, null, "session-2"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("로그인 사용자가 비로그인(세션) 소유 세션을 조회하면 FORBIDDEN 예외를 던진다")
    void 로그인_사용자가_비로그인_세션_FORBIDDEN() {
        User owner = userWithId(1L, "owner@gmail.com");
        ChatSession session = sessionWithId(10L, ChatSession.createForSession("session-1"));
        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatSessionService.findOwned(10L, owner, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
    
    @Test
    @DisplayName("로그인 사용자 세션 생성 시 user 가 설정되고 약관 목록은 비어 있다")
    void 로그인_세션_생성() {
        User owner = userWithId(1L, "owner@gmail.com");
        given(chatSessionRepository.save(any())).willAnswer(invocation -> {
            ChatSession s = invocation.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 100L);
            return s;
        });

        ChatSessionCreateResponse response = chatSessionService.create(owner, null);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.claimCriteria()).isEmpty();

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        then(chatSessionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(owner);
        assertThat(captor.getValue().getSessionKey()).isNull();
    }

    @Test
    @DisplayName("비로그인 사용자 세션 생성 시 session_key 가 설정된다")
    void 비로그인_세션_생성() {
        given(chatSessionRepository.save(any())).willAnswer(invocation -> {
            ChatSession s = invocation.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 100L);
            return s;
        });

        ChatSessionCreateResponse response = chatSessionService.create(null, "session-1");

        assertThat(response.id()).isEqualTo(100L);

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        then(chatSessionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getSessionKey()).isEqualTo("session-1");
    }

    @Test
    @DisplayName("목록: 세션별 약관 요약과 마지막 메시지를 조립하고, 메시지 없는 세션의 lastMessage 는 null 이다")
    void 목록_조립() {
        User owner = userWithId(1L, "owner@gmail.com");
        ChatSession session1 = sessionWithId(10L, ChatSession.createForUser(owner));
        ChatSession session2 = sessionWithId(20L, ChatSession.createForUser(owner));
        ClaimCriteria c1 = criteriaWithId(100L, ClaimCriteria.createForUser(owner));
        ClaimCriteria c2 = criteriaWithId(200L, ClaimCriteria.createForUser(owner));

        given(chatSessionRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(session1, session2));
        given(chatSessionClaimCriteriaRepository.findByChatSessionIdInFetchClaimCriteria(any()))
                .willReturn(List.of(
                        ChatSessionClaimCriteria.create(session1, c1),
                        ChatSessionClaimCriteria.create(session2, c2)));
        given(documentRepository.findByClaimCriteriaIdIn(any()))
                .willReturn(List.of(
                        Document.create(c1, "a.pdf", 100L),
                        Document.create(c2, "b.pdf", 200L)));
        given(chatMessageRepository.findLastMessagesByChatSessionIds(any()))
                .willReturn(List.of(ChatMessage.create(session1, MessageRole.ASSISTANT, "마지막 답변")));

        List<ChatSessionListResponse> result = chatSessionService.findList(1L);

        assertThat(result).hasSize(2);

        ChatSessionListResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(10L);
        assertThat(first.lastMessage()).isEqualTo("마지막 답변");
        assertThat(first.claimCriteria()).singleElement()
                .satisfies(summary -> {
                    assertThat(summary.id()).isEqualTo(100L);
                    assertThat(summary.fileName()).isEqualTo("a.pdf");
                });

        ChatSessionListResponse second = result.get(1);
        assertThat(second.id()).isEqualTo(20L);
        assertThat(second.lastMessage()).isNull();
        assertThat(second.claimCriteria()).singleElement()
                .satisfies(summary -> assertThat(summary.fileName()).isEqualTo("b.pdf"));
    }

    @Test
    @DisplayName("목록: 세션이 없으면 빈 리스트를 반환하고 추가 조회를 하지 않는다")
    void 목록_빈_결과() {
        given(chatSessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        List<ChatSessionListResponse> result = chatSessionService.findList(1L);

        assertThat(result).isEmpty();
        then(chatSessionClaimCriteriaRepository).shouldHaveNoInteractions();
        then(chatMessageRepository).shouldHaveNoInteractions();
    }
    
    @Test
    @DisplayName("상세: 연결 약관(파일명+상태)과 메시지 이력을 조립한다")
    void 상세_조립() {
        User owner = userWithId(1L, "owner@gmail.com");
        ChatSession session = sessionWithId(10L, ChatSession.createForUser(owner));
        ClaimCriteria criteria = criteriaWithId(100L, ClaimCriteria.createForUser(owner));
        criteria.updateStatus(ClaimCriteriaStatus.COMPLETED);

        given(chatSessionRepository.findById(10L)).willReturn(Optional.of(session));
        given(chatSessionClaimCriteriaRepository.findByChatSessionIdInFetchClaimCriteria(List.of(10L)))
                .willReturn(List.of(ChatSessionClaimCriteria.create(session, criteria)));
        given(documentRepository.findByClaimCriteriaIdIn(any()))
                .willReturn(List.of(Document.create(criteria, "약관.pdf", 100L)));
        given(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(10L))
                .willReturn(List.of(
                        ChatMessage.create(session, MessageRole.USER, "질문"),
                        ChatMessage.create(session, MessageRole.ASSISTANT, "답변")));

        ChatSessionDetailResponse result = chatSessionService.findDetail(10L, owner, null);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.claimCriteria()).singleElement()
                .satisfies(detail -> {
                    assertThat(detail.id()).isEqualTo(100L);
                    assertThat(detail.fileName()).isEqualTo("약관.pdf");
                    assertThat(detail.status()).isEqualTo(ClaimCriteriaStatus.COMPLETED);
                });
        assertThat(result.messages())
                .extracting(message -> message.role() + ":" + message.content())
                .containsExactly("USER:질문", "ASSISTANT:답변");
    }
}