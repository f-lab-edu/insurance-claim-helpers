package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.ChatSession;
import com.swk.claimhelpers.chat.entity.MessageRole;
import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import com.swk.claimhelpers.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatMessageRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChatMessageRepository repository;

    @Test
    @DisplayName("세션별로 가장 마지막에 저장된 메시지 한 건씩만 반환한다")
    void 세션별_마지막_메시지만_반환() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session1 = em.persist(ChatSession.createForUser(user));
        ChatSession session2 = em.persist(ChatSession.createForSession("sess-key"));

        em.persist(ChatMessage.create(session1, MessageRole.USER, "s1-첫번째"));
        em.persist(ChatMessage.create(session1, MessageRole.ASSISTANT, "s1-마지막"));
        em.persist(ChatMessage.create(session2, MessageRole.USER, "s2-유일"));
        em.flush();
        em.clear();

        List<ChatMessage> result = repository.findLastMessagesByChatSessionIds(
                List.of(session1.getId(), session2.getId()));

        Map<Long,String> bySession = result.stream()
                .collect(Collectors.toMap(m -> m.getChatSession().getId(), ChatMessage::getContent));

        assertThat(result).hasSize(2);
        assertThat(bySession.get(session1.getId())).isEqualTo("s1-마지막");
        assertThat(bySession.get(session2.getId())).isEqualTo("s2-유일");
    }

    @Test
    @DisplayName("메시지가 없는 세션은 결과에 포함되지 않는다")
    void 메시지_없는_세션은_제외() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession withMessage = em.persist(ChatSession.createForUser(user));
        ChatSession empty = em.persist(ChatSession.createForUser(user));

        em.persist(ChatMessage.create(withMessage, MessageRole.USER, "안녕하세요"));
        em.flush();
        em.clear();

        List<ChatMessage> result = repository.findLastMessagesByChatSessionIds(
                List.of(withMessage.getId(), empty.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatSession().getId()).isEqualTo(withMessage.getId());
    }

    @Test
    @DisplayName("세션의 최근 N개 메시지를 최신순(id 내림차순)으로 반환한다")
    void 세션의_최근_N개_메시지를_최신순으로_반환() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session = em.persist(ChatSession.createForUser(user));
        ChatSession other = em.persist(ChatSession.createForSession("other-key"));
        em.persist(ChatMessage.create(other, MessageRole.USER, "다른세션-메시지"));

        for(int i = 1; i <= 12; i++) {
            em.persist(ChatMessage.create(session, MessageRole.USER, "msg-" + i));
        }
        em.flush();
        em.clear();

        List<ChatMessage> result = repository.findByChatSessionIdOrderByIdDesc(
                session.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(10);
        assertThat(result.get(0).getContent()).isEqualTo("msg-12");
        assertThat(result.get(9).getContent()).isEqualTo("msg-3");
    }
}