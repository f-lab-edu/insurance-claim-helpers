package com.swk.claimhelpers.policy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TextNormalizer 의 청킹 전 텍스트 정규화(apply) 검증.
 */
class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    @DisplayName("NUL(0x00) 문자를 제거한다")
    void NUL_제거() {
        String nul = String.valueOf((char) 0);

        List<Document> result = normalizer.apply(List.of(new Document("본" + nul + "문" + nul + "내용")));

        assertThat(result.get(0).getText()).isEqualTo("본문내용");
    }

    @Test
    @DisplayName("연속 공백·개행을 단일 스페이스로 축약하고 앞뒤 공백을 제거한다")
    void 공백_정규화() {
        List<Document> result = normalizer.apply(List.of(new Document("  제1조   목적 \n \n본문   내용  ")));

        assertThat(result.get(0).getText()).isEqualTo("제1조 목적 본문 내용");
    }

    @Test
    @DisplayName("정규화해도 metadata 는 보존한다")
    void metadata_보존() {
        Document doc = new Document("내용", Map.of("k", "v"));

        List<Document> result = normalizer.apply(List.of(doc));

        assertThat(result.get(0).getMetadata()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("metadata 값이 null 인 항목은 제외한다 (Document 생성자가 null 값을 거부)")
    void metadata_null값_제외() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("k", "v");
        Document doc = new Document("내용", metadata);
        doc.getMetadata().put("nullKey", null);

        List<Document> result = normalizer.apply(List.of(doc));

        assertThat(result.get(0).getMetadata()).containsEntry("k", "v");
        assertThat(result.get(0).getMetadata()).doesNotContainKey("nullKey");
    }
}