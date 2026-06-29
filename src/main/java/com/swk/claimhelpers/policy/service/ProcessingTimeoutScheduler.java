package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingTimeoutScheduler {

    private static final Duration TIMEOUT = Duration.ofHours(1);

    private final ClaimCriteriaRepository claimCriteriaRepository;

    // 매일 새벽 4시 1회 실행
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "failTimedOutProcessing", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Transactional
    public void failTimedOutProcessing() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minus(TIMEOUT);

        int failed = claimCriteriaRepository.failTimedOutProcessing(threshold, now);
        if (failed > 0) {
            log.warn("처리 시간 초과 PROCESSING 약관 {}건을 FAILED 로 변경", failed);
        }
    }
}