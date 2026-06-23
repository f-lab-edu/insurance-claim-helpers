package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface ClaimCriteriaRepository extends JpaRepository<ClaimCriteria, Long> {

    List<ClaimCriteria> findByUserId(Long userId);

    List<ClaimCriteria> findBySessionKey(String sessionKey);
    
    @Modifying
    @Query("UPDATE ClaimCriteria c "
            + "SET c.status = com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus.FAILED, c.updatedAt = :now "
            + "WHERE c.status = com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus.PROCESSING "
            + "AND c.updatedAt < :threshold")
    int failTimedOutProcessing(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);
}