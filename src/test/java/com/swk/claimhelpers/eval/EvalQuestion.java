package com.swk.claimhelpers.eval;

/**
 * 평가용 질문 한 건.
 *
 * @param id         질문 식별자(리포트 표기용)
 * @param question   사용자 질문 문구
 * @param answerable true=약관에 근거가 있는 답변형, false=약관에 없는 함정형(기권해야 정답)
 */
public record EvalQuestion(String id, String question, boolean answerable) {
}