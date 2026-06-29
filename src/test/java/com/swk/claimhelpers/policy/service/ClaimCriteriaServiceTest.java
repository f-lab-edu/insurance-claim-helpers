package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.chat.repository.ChatSessionClaimCriteriaRepository;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClaimCriteriaServiceTest {

    @Mock
    private ClaimCriteriaRepository claimCriteriaRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private S3FileStorage s3FileStorage;

    @Mock
    private ChatSessionClaimCriteriaRepository chatSessionClaimCriteriaRepository;

    @InjectMocks
    private ClaimCriteriaService claimCriteriaService;

    private User userWithId(long id, String email) {
        User user = User.create(email);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ClaimCriteria criteriaWithId(long id, ClaimCriteria criteria) {
        ReflectionTestUtils.setField(criteria, "id", id);
        return criteria;
    }

    @Test
    @DisplayName("로그인 사용자가 본인 약관을 조회하면 엔티티를 반환한다")
    void 로그인_본인_성공() {
        User owner = userWithId(1L, "owner@gmail.com");
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForUser(owner));
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));

        ClaimCriteria result = claimCriteriaService.findOwned(10L, owner, null);

        assertThat(result).isSameAs(criteria);
    }

    @Test
    @DisplayName("존재하지 않는 id 면 CLAIM_CRITERIA_NOT_FOUND 예외를 던진다")
    void 없는_id_NOT_FOUND() {
        User owner = userWithId(1L, "owner@gmail.com");
        given(claimCriteriaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> claimCriteriaService.findOwned(99L, owner, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLAIM_CRITERIA_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 사용자가 다른 사용자의 약관을 조회하면 FORBIDDEN 예외를 던진다")
    void 다른_user_FORBIDDEN() {
        User owner = userWithId(1L, "owner@gmail.com");
        User other = userWithId(2L, "other@gmail.com");
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForUser(owner));
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));

        assertThatThrownBy(() -> claimCriteriaService.findOwned(10L, other, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("비로그인 사용자가 본인 세션 약관을 조회하면 엔티티를 반환한다")
    void 비로그인_본인_성공() {
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForSession("session-1"));
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));

        ClaimCriteria result = claimCriteriaService.findOwned(10L, null, "session-1");

        assertThat(result).isSameAs(criteria);
    }

    @Test
    @DisplayName("비로그인 사용자가 다른 세션의 약관을 조회하면 FORBIDDEN 예외를 던진다")
    void 다른_session_key_FORBIDDEN() {
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForSession("session-1"));
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));

        assertThatThrownBy(() -> claimCriteriaService.findOwned(10L, null, "session-2"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("로그인 사용자가 비로그인(세션) 약관을 조회하면 FORBIDDEN 예외를 던진다")
    void 로그인_사용자가_비로그인_약관_FORBIDDEN() {
        User owner = userWithId(1L, "owner@gmail.com");
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForSession("session-1"));
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));

        assertThatThrownBy(() -> claimCriteriaService.findOwned(10L, owner, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private Document documentWith(ClaimCriteria criteria, String objectKey) {
        Document document = Document.create(criteria, "약관.pdf", 1000L);
        if (objectKey != null) {
            document.updateObjectKey(objectKey);
        }
        return document;
    }

    @Test
    @DisplayName("삭제: vector_store 청크 + S3 객체 + chat 링크 + documents + claim_criteria 를 모두 삭제한다")
    void 삭제_정상() {
        User owner = userWithId(1L, "owner@gmail.com");
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForUser(owner));
        Document document = documentWith(criteria, "claim-criteria/10/key.pdf");
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));
        given(documentRepository.findByClaimCriteriaId(10L)).willReturn(Optional.of(document));

        claimCriteriaService.delete(10L, owner, null);

        then(vectorStore).should().delete("claim_criteria_id == 10");
        then(s3FileStorage).should().delete("claim-criteria/10/key.pdf");
        then(chatSessionClaimCriteriaRepository).should().deleteByClaimCriteriaId(10L);
        then(documentRepository).should().delete(document);
        then(claimCriteriaRepository).should().delete(criteria);
    }

    @Test
    @DisplayName("삭제: object_key 가 없으면 S3 삭제는 건너뛰고 나머지는 삭제한다")
    void 삭제_object_key_없음() {
        User owner = userWithId(1L, "owner@gmail.com");
        ClaimCriteria criteria = criteriaWithId(10L, ClaimCriteria.createForUser(owner));
        Document document = documentWith(criteria, null);
        given(claimCriteriaRepository.findById(10L)).willReturn(Optional.of(criteria));
        given(documentRepository.findByClaimCriteriaId(10L)).willReturn(Optional.of(document));

        claimCriteriaService.delete(10L, owner, null);

        then(s3FileStorage).should(never()).delete(anyString());
        then(vectorStore).should().delete("claim_criteria_id == 10");
        then(chatSessionClaimCriteriaRepository).should().deleteByClaimCriteriaId(10L);
        then(documentRepository).should().delete(document);
        then(claimCriteriaRepository).should().delete(criteria);
    }

    @Test
    @DisplayName("삭제: 존재하지 않는 약관이면 아무것도 삭제하지 않고 NOT_FOUND")
    void 삭제_없는_약관() {
        User owner = userWithId(1L, "owner@gmail.com");
        given(claimCriteriaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> claimCriteriaService.delete(99L, owner, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLAIM_CRITERIA_NOT_FOUND);

        then(vectorStore).should(never()).delete(anyString());
        then(s3FileStorage).should(never()).delete(anyString());
        then(chatSessionClaimCriteriaRepository).should(never()).deleteByClaimCriteriaId(any());
        then(documentRepository).should(never()).delete(any(Document.class));
        then(claimCriteriaRepository).should(never()).delete(any(ClaimCriteria.class));
    }
}