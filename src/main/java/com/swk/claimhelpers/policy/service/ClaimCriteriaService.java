package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.chat.repository.ChatSessionClaimCriteriaRepository;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClaimCriteriaService {

    private final ClaimCriteriaRepository claimCriteriaRepository;
    private final DocumentRepository documentRepository;
    private final ChatSessionClaimCriteriaRepository chatSessionClaimCriteriaRepository;
    private final VectorStore vectorStore;
    private final S3FileStorage s3FileStorage;

    /**
     * @param user       로그인 사용자(없으면 null)
     * @param sessionKey 비로그인 식별 키(로그인 시 null)
     */
    @Transactional(readOnly = true)
    public ClaimCriteria findOwned(Long id, User user, String sessionKey) {
        ClaimCriteria criteria = claimCriteriaRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CLAIM_CRITERIA_NOT_FOUND));

        if (user != null) {
            // 로그인 사용자
            User owner = criteria.getUser();
            if (owner == null || !Objects.equals(owner.getId(), user.getId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        } else {
            // 비로그인 사용자
            if (!Objects.equals(criteria.getSessionKey(), sessionKey)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }

        return criteria;
    }
    
    @Transactional(readOnly = true)
    public List<Document> findCompletedByUserId(Long userId) {
        return documentRepository.findByOwnerUserIdAndStatus(userId, ClaimCriteriaStatus.COMPLETED);
    }

    /**
     * 삭제 순서:
     *   1) vector_store 청크
     *   2) S3 객체 (외부 리소스, object_key 가 있을 때만)
     *   3) chat_session_claim_criteria 링크 (claim_criteria 보다 먼저 — FK)
     *   4) documents (claim_criteria 보다 먼저 — FK)
     *   5) claim_criteria
     */
    @Transactional
    public void delete(Long id, User user, String sessionKey) {
        ClaimCriteria criteria = findOwned(id, user, sessionKey);
        Document document = documentRepository.findByClaimCriteriaId(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));

        vectorStore.delete(ClaimCriteriaVectorMetadata.CLAIM_CRITERIA_ID + " == " + id);

        if (document.getObjectKey() != null) {
            s3FileStorage.delete(document.getObjectKey());
        }

        chatSessionClaimCriteriaRepository.deleteByClaimCriteriaId(id);
        documentRepository.delete(document);
        claimCriteriaRepository.delete(criteria);
    }
}