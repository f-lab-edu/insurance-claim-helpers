package com.swk.claimhelpers.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxConcurrent 동시에 처리(=메모리 적재)할 약관 최대 개수. core=max 로 설정
 * @param queueCapacity 풀이 꽉 찼을 때 대기시킬 작업 수. 초과분은 거부(→ status FAILED)
 */
@ConfigurationProperties(prefix = "embedding.executor")
public record EmbeddingExecutorProperties(
        int maxConcurrent,
        int queueCapacity
) {
}