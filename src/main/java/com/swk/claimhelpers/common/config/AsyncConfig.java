package com.swk.claimhelpers.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 약관 임베딩 작업 전용 스레드풀
 * - 포화 시 거부 정책은 AbortPolicy(기본) 그대로.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(EmbeddingExecutorProperties.class)
public class AsyncConfig {
    
    @Bean("embeddingExecutor")
    @DependsOn("entityManagerFactory")
    public TaskExecutor embeddingExecutor(EmbeddingExecutorProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.maxConcurrent());
        executor.setMaxPoolSize(props.maxConcurrent());
        executor.setQueueCapacity(props.queueCapacity());
        executor.setThreadNamePrefix("embedding-");
        // 포화 시 거부 → 호출 스레드로 TaskRejectedException 전파(컨트롤러가 FAILED 처리)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        // 대기 큐의 미시작 작업은 드롭
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}