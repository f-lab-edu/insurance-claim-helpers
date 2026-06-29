package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProcessingTimeoutSchedulerTest {

    @Mock
    private ClaimCriteriaRepository claimCriteriaRepository;

    @InjectMocks
    private ProcessingTimeoutScheduler scheduler;

    @Test
    @DisplayName("threshold 를 현재보다 1시간 이전으로 잡아 회수 쿼리에 위임한다")
    void 시간초과_PROCESSING_처리() {
        scheduler.failTimedOutProcessing();

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(claimCriteriaRepository).should()
                .failTimedOutProcessing(thresholdCaptor.capture(), nowCaptor.capture());

        Duration gap = Duration.between(thresholdCaptor.getValue(), nowCaptor.getValue());
        assertThat(gap).isCloseTo(Duration.ofHours(1), Duration.ofSeconds(5));
    }
}