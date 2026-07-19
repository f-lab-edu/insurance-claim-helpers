package com.swk.claimhelpers.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvalReportTest {

    @Test
    void 답변형과_함정형_행이_통과수와_함께_출력된다() {
        EvalReport report = new EvalReport();
        // 답변형: N=5, faith 5, answerRel 5, contextRel 4회(precision 합 3.0 → 평균 0.6)
        report.add(new QuestionOutcome("Q1", true, 5, 5, 5, 4, 3.0, 0));
        // 함정형: N=5, 기권 5회
        report.add(new QuestionOutcome("Q7", false, 5, 0, 0, 0, 0.0, 5));

        String out = report.format();

        assertThat(out).contains("Q1").contains("Q7");
        assertThat(out).contains("답변형").contains("함정형");
        assertThat(out).contains("5/5");          // faithfulness 등
        assertThat(out).contains("4/5");          // contextRelevance
        assertThat(out).contains("0.60");         // 평균 precision
        assertThat(out).contains("-");
    }
}