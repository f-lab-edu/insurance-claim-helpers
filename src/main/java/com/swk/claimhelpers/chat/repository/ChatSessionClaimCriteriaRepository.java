package com.swk.claimhelpers.chat.repository;

import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteria;
import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteriaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ChatSessionClaimCriteriaRepository extends JpaRepository<ChatSessionClaimCriteria, ChatSessionClaimCriteriaId> {

    List<ChatSessionClaimCriteria> findByChatSessionId(Long chatSessionId);
    
    @Modifying
    @Query("DELETE FROM ChatSessionClaimCriteria c WHERE c.claimCriteria.id = :claimCriteriaId")
    void deleteByClaimCriteriaId(@Param("claimCriteriaId") Long claimCriteriaId);
}