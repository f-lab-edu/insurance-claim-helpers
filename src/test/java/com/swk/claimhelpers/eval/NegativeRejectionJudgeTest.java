package com.swk.claimhelpers.eval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NegativeRejectionJudgeTest {

    private final NegativeRejectionJudge judge =
            new NegativeRejectionJudge(mock(ChatClient.class), mock(Resource.class));

    @Test
    void parseAbstained는_YES면_기권으로_본다() {
        assertThat(judge.parseAbstained("YES")).isTrue();
        assertThat(judge.parseAbstained(" yes ")).isTrue();
        assertThat(judge.parseAbstained("NO")).isFalse();
    }
}