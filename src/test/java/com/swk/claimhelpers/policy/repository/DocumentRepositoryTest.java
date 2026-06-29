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
}