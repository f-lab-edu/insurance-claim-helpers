package com.swk.claimhelpers.policy.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ObjectKeyGenerator {

    public String generate(Long claimCriteriaId) {
        return "claim-criteria/" + claimCriteriaId + "/" + UUID.randomUUID() + ".pdf";
    }
}