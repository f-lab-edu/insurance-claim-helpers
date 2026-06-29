package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSession;
import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import com.swk.claimhelpers.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatSessionClaimCriteriaRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChatSessionClaimCriteriaRepository repository;

    @Test
    @DisplayName("특정 약관에 연결된 링크만 삭제하고 다른 약관 링크는 남긴다")
    void 약관id로_링크만_일괄삭제() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session = em.persist(ChatSession.createForUser(user));
        ClaimCriteria target = em.persist(ClaimCriteria.createForUser(user));
        ClaimCriteria other = em.persist(ClaimCriteria.createForUser(user));
        em.persist(ChatSessionClaimCriteria.create(session, target));
        em.persist(ChatSessionClaimCriteria.create(session, other));
        em.flush();
        em.clear();

        repository.deleteByClaimCriteriaId(target.getId());
        em.clear();

        List<ChatSessionClaimCriteria> remaining = repository.findByChatSessionId(session.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getClaimCriteria().getId()).isEqualTo(other.getId());
    }

    @Test
    @DisplayName("여러 세션의 링크를 약관과 함께 한 번에 조회한다")
    void 여러세션_링크_일괄조회() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session1 = em.persist(ChatSession.createForUser(user));
        ChatSession session2 = em.persist(ChatSession.createForUser(user));
        ClaimCriteria c1 = em.persist(ClaimCriteria.createForUser(user));
        ClaimCriteria c2 = em.persist(ClaimCriteria.createForUser(user));
        em.persist(ChatSessionClaimCriteria.create(session1, c1));
        em.persist(ChatSessionClaimCriteria.create(session2, c2));
        em.flush();
        em.clear();

        List<ChatSessionClaimCriteria> result = repository.findByChatSessionIdInFetchClaimCriteria(
                List.of(session1.getId(), session2.getId()));

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(l -> l.getClaimCriteria().getId())
                .containsExactlyInAnyOrder(c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("특정 세션-약관 링크만 삭제하고 같은 세션의 다른 약관 링크는 남긴다")
    void 세션약관id로_단건_링크삭제() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session = em.persist(ChatSession.createForUser(user));
        ClaimCriteria target = em.persist(ClaimCriteria.createForUser(user));
        ClaimCriteria other = em.persist(ClaimCriteria.createForUser(user));
        em.persist(ChatSessionClaimCriteria.create(session, target));
        em.persist(ChatSessionClaimCriteria.create(session, other));
        em.flush();
        em.clear();

        repository.deleteByChatSessionIdAndClaimCriteriaId(session.getId(), target.getId());
        em.clear();

        List<ChatSessionClaimCriteria> remaining = repository.findByChatSessionId(session.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getClaimCriteria().getId()).isEqualTo(other.getId());
    }

    @Test
    @DisplayName("존재하지 않는 링크를 삭제해도 예외 없이 0건 처리한다(멱등)")
    void 미존재_링크삭제_멱등() {
        User user = em.persist(User.create("owner@gmail.com"));
        ChatSession session = em.persist(ChatSession.createForUser(user));
        ClaimCriteria criteria = em.persist(ClaimCriteria.createForUser(user));
        em.flush();
        em.clear();

        repository.deleteByChatSessionIdAndClaimCriteriaId(session.getId(), criteria.getId());
        em.clear();

        assertThat(repository.findByChatSessionId(session.getId())).isEmpty();
    }
}