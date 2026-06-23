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
}