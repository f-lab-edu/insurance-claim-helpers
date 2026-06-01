package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // claim_criteria_id로 연결된 문서 조회
    Optional<Document> findByClaimCriteriaId(Long claimCriteriaId);
}