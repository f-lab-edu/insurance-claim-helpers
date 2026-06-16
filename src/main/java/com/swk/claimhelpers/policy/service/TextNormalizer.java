package com.swk.claimhelpers.policy.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 청킹 전 텍스트 정규화 단계 -> 청크 수 감소
 */
public class TextNormalizer implements DocumentTransformer {

    private static final String NUL = String.valueOf((char) 0);

    @Override
    public List<Document> apply(List<Document> documents) {
        return documents.stream()
                .map(document -> {
                    String text = document.getText();
                    if (text == null) {
                        return document;
                    }
                    String normalized = text.replace(NUL, "").replaceAll("\\s+", " ").strip();
                    Map<String, Object> metadata = new HashMap<>(document.getMetadata());
                    metadata.values().removeIf(Objects::isNull);
                    return new Document(normalized, metadata);
                })
                .toList();
    }
}