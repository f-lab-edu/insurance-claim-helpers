package com.swk.claimhelpers.eval;

/**
 * 한 질문의 N회 반복 집계 결과.
 * 답변형은 faithfulness/answerRelevance/contextRelevance 를, 함정형은 rejection 을 사용한다.
 *
 * @param runs                 반복 횟수 N
 * @param faithfulnessPass     Faithfulness isPass=true 횟수 (답변형)
 * @param answerRelevancePass  Answer Relevance isPass=true 횟수 (답변형)
 * @param contextRelevancePass Context precision ≥ 0.5 인 횟수 (답변형)
 * @param contextPrecisionSum  회차별 precision 합(평균 산출용, 답변형)
 * @param rejectionPass        기권 성공 횟수 (함정형)
 */
public record QuestionOutcome(
        String id,
        boolean answerable,
        int runs,
        int faithfulnessPass,
        int answerRelevancePass,
        int contextRelevancePass,
        double contextPrecisionSum,
        int rejectionPass) {
}