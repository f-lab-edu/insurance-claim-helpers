package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClaimCriteriaRepository claimCriteriaRepository;

    @Autowired
    private UserRepository userRepository;

    private Document saveClaimWithDocument(User user, ClaimCriteriaStatus status, String fileName, long fileSize) {
        ClaimCriteria criteria = ClaimCriteria.createForUser(user);
        criteria.updateStatus(status);
        claimCriteriaRepository.save(criteria);
        Document document = Document.create(criteria, fileName, fileSize);
        return documentRepository.save(document);
    }

    @Test
    @DisplayName("로그인 사용자의 COMPLETED 약관만, 문서 정보와 함께 반환한다")
    void COMPLETED_약관만_반환() {
        User owner = userRepository.save(User.create("owner@gmail.com"));
        User other = userRepository.save(User.create("other@gmail.com"));

        Document completed = saveClaimWithDocument(owner, ClaimCriteriaStatus.COMPLETED, "완료.pdf", 1000L);
        saveClaimWithDocument(owner, ClaimCriteriaStatus.PROCESSING, "처리중.pdf", 2000L); // 제외
        saveClaimWithDocument(other, ClaimCriteriaStatus.COMPLETED, "남의것.pdf", 3000L);  // 제외

        List<Document> result = documentRepository.findByOwnerUserIdAndStatus(
                owner.getId(), ClaimCriteriaStatus.COMPLETED);

        assertThat(result).hasSize(1);
        Document found = result.get(0);
        assertThat(found.getId()).isEqualTo(completed.getId());
        assertThat(found.getFileName()).isEqualTo("완료.pdf");
        assertThat(found.getFileSize()).isEqualTo(1000L);
        assertThat(found.getClaimCriteria().getStatus()).isEqualTo(ClaimCriteriaStatus.COMPLETED);
    }

    @Test
    @DisplayName("여러 약관 id로 문서를 일괄 조회하며 약관(status 포함)을 함께 로딩한다")
    void 약관id_목록으로_문서_일괄조회() {
        User user = userRepository.save(User.create("owner@gmail.com"));

        Document doc1 = saveClaimWithDocument(user, ClaimCriteriaStatus.COMPLETED, "a.pdf", 100L);
        Document doc2 = saveClaimWithDocument(user, ClaimCriteriaStatus.PROCESSING, "b.pdf", 200L);
        saveClaimWithDocument(user, ClaimCriteriaStatus.COMPLETED, "c.pdf", 300L); // 조회 대상 아님

        List<Long> ids = List.of(
                doc1.getClaimCriteria().getId(),
                doc2.getClaimCriteria().getId());

        List<Document> result = documentRepository.findByClaimCriteriaIdIn(ids);

        assertThat(result)
                .extracting(Document::getFileName)
                .containsExactlyInAnyOrder("a.pdf", "b.pdf");
        assertThat(result)
                .extracting(d -> d.getClaimCriteria().getStatus())
                .containsExactlyInAnyOrder(ClaimCriteriaStatus.COMPLETED, ClaimCriteriaStatus.PROCESSING);
    }
}