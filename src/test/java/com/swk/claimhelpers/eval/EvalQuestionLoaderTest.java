package com.swk.claimhelpers.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalQuestionLoaderTest {

    @Test
    void 질문세트를_로드하면_8건이_유형과_함께_읽힌다() {
        List<EvalQuestion> questions = new EvalQuestionLoader().load();

        // 총 8건, 답변형 6 + 함정형 2
        assertThat(questions).hasSize(8);
        assertThat(questions).filteredOn(EvalQuestion::answerable).hasSize(6);
        assertThat(questions).filteredOn(q -> !q.answerable()).hasSize(2);
        // id/문구가 비어있지 않다
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.id()).isNotBlank();
            assertThat(q.question()).isNotBlank();
        });
    }
}