package com.swk.claimhelpers.eval;

import java.util.ArrayList;
import java.util.List;

public class EvalReport {

    private final List<QuestionOutcome> outcomes = new ArrayList<>();

    public void add(QuestionOutcome outcome) {
        outcomes.add(outcome);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAG 평가 리포트 ===\n");
        // 컬럼: 질문 / 유형 / Faithfulness / AnswerRel / ContextRel(평균precision) / Rejection
        sb.append(String.format("%-6s %-8s %-14s %-12s %-18s %-10s%n",
                "질문", "유형", "Faithfulness", "AnswerRel", "ContextRel", "Rejection"));
        for(QuestionOutcome o : outcomes) {
            if(o.answerable()) {
                double avgPrecision = o.runs() == 0 ? 0.0 : o.contextPrecisionSum() / o.runs();
                sb.append(String.format("%-6s %-8s %-14s %-12s %-18s %-10s%n",
                        o.id(),
                        "답변형",
                        fraction(o.faithfulnessPass(), o.runs()),
                        fraction(o.answerRelevancePass(), o.runs()),
                        fraction(o.contextRelevancePass(), o.runs()) + String.format("(%.2f)", avgPrecision),
                        "-"));
            } else {
                sb.append(String.format("%-6s %-8s %-14s %-12s %-18s %-10s%n",
                        o.id(),
                        "함정형",
                        "-",
                        "-",
                        "-",
                        fraction(o.rejectionPass(), o.runs())));
            }
        }
        return sb.toString();
    }

    private String fraction(int pass, int runs) {
        return pass + "/" + runs;
    }
}