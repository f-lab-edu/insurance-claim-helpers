package com.swk.claimhelpers.eval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContextRelevanceJudgeTest {

    private final ContextRelevanceJudge judge =
            new ContextRelevanceJudge(mock(ChatClient.class), mock(Resource.class));

    @Test
    void parseVerdict는_YES만_true로_본다() {
        assertThat(judge.parseVerdict("YES")).isTrue();
        assertThat(judge.parseVerdict(" yes\n")).isTrue();
        assertThat(judge.parseVerdict("NO")).isFalse();
        assertThat(judge.parseVerdict("관련 없음")).isFalse();
    }

    @Test
    void precision은_관련청크_비율을_계산한다() {
        assertThat(judge.precision(List.of(true, false, true, true, false))).isEqualTo(0.6);
    }

    @Test
    void precision은_빈목록이면_0이다() {
        assertThat(judge.precision(List.of())).isEqualTo(0.0);
    }
}