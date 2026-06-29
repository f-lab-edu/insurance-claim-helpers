package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClaimCriteriaRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClaimCriteriaRepository repository;

    private Long persistWithUpdatedAt(ClaimCriteriaStatus status, LocalDateTime updatedAt) {
        ClaimCriteria criteria = ClaimCriteria.createForSession("session-1");
        criteria.updateStatus(status);
        em.persist(criteria);
        em.flush();
        em.getEntityManager()
                .createNativeQuery("UPDATE claim_criteria SET updated_at = :ts WHERE id = :id")
                .setParameter("ts", updatedAt)
                .setParameter("id", criteria.getId())
                .executeUpdate();
        return criteria.getId();
    }

    @Test
    @DisplayName("1시간 넘게 PROCESSING 인 약관만 FAILED 로 회수한다")
    void 잔류_PROCESSING_회수() {
        LocalDateTime now = LocalDateTime.now();
        Long stuck = persistWithUpdatedAt(ClaimCriteriaStatus.PROCESSING, now.minusHours(2));
        Long recent = persistWithUpdatedAt(ClaimCriteriaStatus.PROCESSING, now.minusMinutes(10));
        Long completed = persistWithUpdatedAt(ClaimCriteriaStatus.COMPLETED, now.minusHours(2));
        em.clear();

        int reclaimed = repository.failTimedOutProcessing(now.minusHours(1), now);

        em.clear();
        assertThat(reclaimed).isEqualTo(1);
        assertThat(repository.findById(stuck)).get()
                .extracting(ClaimCriteria::getStatus).isEqualTo(ClaimCriteriaStatus.FAILED);
        assertThat(repository.findById(recent)).get()
                .extracting(ClaimCriteria::getStatus).isEqualTo(ClaimCriteriaStatus.PROCESSING);
        assertThat(repository.findById(completed)).get()
                .extracting(ClaimCriteria::getStatus).isEqualTo(ClaimCriteriaStatus.COMPLETED);
    }
}