package com.swk.claimhelpers.policy.config;

import com.swk.claimhelpers.policy.service.TextNormalizer;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 약관 청킹 파이프라인 구성.
 */
@Configuration
public class ChunkingConfig {

    @Bean
    public DocumentTransformer chunkingPipeline() {
        TextNormalizer normalizer = new TextNormalizer();
        TokenTextSplitter tokenSplitter = new TokenTextSplitter();
        return documents -> tokenSplitter.apply(normalizer.apply(documents));

        // [개선 예정] 조(條) 단위 분할 합성 — 과분할 이슈 해결 후 재활성화
        // ClauseSplitter clauseSplitter = new ClauseSplitter();
        // return documents -> tokenSplitter.apply(clauseSplitter.apply(normalizer.apply(documents)));
    }
}