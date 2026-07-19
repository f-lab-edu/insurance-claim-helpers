package com.swk.claimhelpers.eval;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class EvalQuestionLoader {

    private static final String RESOURCE_PATH = "/eval/eval-questions.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<EvalQuestion> load() {
        try(InputStream in = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if(in == null) {
                throw new IllegalStateException("질문 세트 리소스를 찾을 수 없습니다: " + RESOURCE_PATH);
            }
            EvalQuestion[] array = objectMapper.readValue(in, EvalQuestion[].class);
            return List.of(array);
        } catch(IOException e) {
            throw new UncheckedIOException("질문 세트 로드 실패: " + RESOURCE_PATH, e);
        }
    }
}