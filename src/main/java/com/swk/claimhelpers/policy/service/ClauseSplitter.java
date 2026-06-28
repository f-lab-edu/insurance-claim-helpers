package com.swk.claimhelpers.policy.service;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 약관 본문을 "조(條)" 단위로 분할하는 청킹 스텝.
 */
public class ClauseSplitter extends TextSplitter {

    // "제1조", "제 12 조" 등 조항 시작 경계. 숫자 양옆 공백 변형을 허용
    private static final Pattern CLAUSE_BOUNDARY = Pattern.compile("제\\s*\\d+\\s*조");

    @Override
    protected List<String> splitText(String text) {
        Matcher matcher = CLAUSE_BOUNDARY.matcher(text);
        List<Integer> boundaries = new ArrayList<>();
        while (matcher.find()) {
            boundaries.add(matcher.start());
        }

        if (boundaries.isEmpty()) {
            return List.of(text);
        }

        // 첫 조 앞 텍스트(서문 등)도 버리지 않고 0부터 한 청크로 보존한다.
        List<Integer> starts = new ArrayList<>();
        if (boundaries.get(0) != 0) {
            starts.add(0);
        }
        starts.addAll(boundaries);

        // 각 청크를 [현재 시작, 다음 시작 직전) 범위로 잘라낸다.
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();
            String chunkText = text.substring(start, end).strip();
            if (!chunkText.isEmpty()) {
                chunks.add(chunkText);
            }
        }
        return chunks;
    }
}