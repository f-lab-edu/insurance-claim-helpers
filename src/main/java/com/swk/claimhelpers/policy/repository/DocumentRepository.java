package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // claim_criteria_id로 연결된 문서 조회
    Optional<Document> findByClaimCriteriaId(Long claimCriteriaId);

    // 로그인 사용자의 특정 상태 약관 목록 조회.
    @Query("SELECT d FROM Document d JOIN FETCH d.claimCriteria c "
            + "WHERE c.user.id = :userId AND c.status = :status "
            + "ORDER BY c.createdAt DESC")
    List<Document> findByOwnerUserIdAndStatus(@Param("userId") Long userId,
                                              @Param("status") ClaimCriteriaStatus status);
}