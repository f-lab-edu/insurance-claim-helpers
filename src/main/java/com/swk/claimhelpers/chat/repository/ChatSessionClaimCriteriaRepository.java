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

    @Query("SELECT l FROM ChatSessionClaimCriteria l JOIN FETCH l.claimCriteria "
            + "WHERE l.chatSession.id IN :sessionIds")
    List<ChatSessionClaimCriteria> findByChatSessionIdInFetchClaimCriteria(@Param("sessionIds") Collection<Long> sessionIds);

    @Modifying
    @Query("DELETE FROM ChatSessionClaimCriteria c WHERE c.claimCriteria.id = :claimCriteriaId")
    void deleteByClaimCriteriaId(@Param("claimCriteriaId") Long claimCriteriaId);
    
    @Modifying
    @Query("DELETE FROM ChatSessionClaimCriteria c "
            + "WHERE c.chatSession.id = :chatSessionId AND c.claimCriteria.id = :claimCriteriaId")
    void deleteByChatSessionIdAndClaimCriteriaId(@Param("chatSessionId") Long chatSessionId,
                                                 @Param("claimCriteriaId") Long claimCriteriaId);
}