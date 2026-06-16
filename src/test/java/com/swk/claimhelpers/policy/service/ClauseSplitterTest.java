package com.swk.claimhelpers.policy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClauseSplitter 의 조 단위 분할 로직(splitText) 검증. (추후 청킹 전략 변경 시 사용)
 */
class ClauseSplitterTest {

    private final ClauseSplitter splitter = new ClauseSplitter();

//    @Test
    @DisplayName("여러 '제N조'가 있으면 조 단위로 분할한다")
    void 여러_조를_조_단위로_분할한다() {
        String text = "제1조 목적 이 약관은 ... 제2조 정의 여기서 말하는 ... 제3조 보장내용 보험금은 ...";

        List<String> chunks = splitter.splitText(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).startsWith("제1조");
        assertThat(chunks.get(1)).startsWith("제2조");
        assertThat(chunks.get(2)).startsWith("제3조");
    }

//    @Test
    @DisplayName("첫 조 앞 서문도 버리지 않고 한 청크로 보존한다")
    void 첫_조_앞_서문을_보존한다() {
        String text = "보험약관 안내문입니다. 제1조 목적 이 약관은 ... 제2조 정의 ...";

        List<String> chunks = splitter.splitText(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).startsWith("보험약관 안내문");
        assertThat(chunks.get(1)).startsWith("제1조");
        assertThat(chunks.get(2)).startsWith("제2조");
    }

//    @Test
    @DisplayName("'제N조'가 하나도 없으면 입력 전체를 한 청크로 반환한다")
    void 조가_없으면_입력_전체를_한_청크로_반환한다() {
        String text = "조 경계가 전혀 없는 일반 텍스트입니다.";

        List<String> chunks = splitter.splitText(text);

        assertThat(chunks).containsExactly(text);
    }

//    @Test
    @DisplayName("'제 12 조'처럼 숫자 양옆 공백 변형도 조 경계로 인식한다")
    void 공백_변형_조_경계도_인식한다() {
        String text = "제 1 조 목적 ... 제 12 조 정의 ...";

        List<String> chunks = splitter.splitText(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).startsWith("제 1 조");
        assertThat(chunks.get(1)).startsWith("제 12 조");
    }
}