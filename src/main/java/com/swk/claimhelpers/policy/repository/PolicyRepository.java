package com.swk.claimhelpers.policy.repository;

import com.swk.claimhelpers.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByUserId(UUID userId);

    List<Policy> findBySessionKey(String sessionKey);
}