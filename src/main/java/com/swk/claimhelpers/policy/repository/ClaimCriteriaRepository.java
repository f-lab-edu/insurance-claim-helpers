package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ClaimCriteriaRepository extends JpaRepository<ClaimCriteria, Long> {

    List<ClaimCriteria> findByUserId(Long userId);

    List<ClaimCriteria> findBySessionKey(String sessionKey);
}