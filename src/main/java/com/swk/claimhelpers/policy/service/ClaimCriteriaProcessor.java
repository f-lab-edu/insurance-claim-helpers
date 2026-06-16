package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S3 에서 PDF 를 다시 받아 텍스트 추출 → 청크 분할 → OpenAI 임베딩 → vector_store 저장까지 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimCriteriaProcessor {

    private static final String METADATA_CLAIM_CRITERIA_ID = "claim_criteria_id";

    private final DocumentRepository documentRepository;
    private final ClaimCriteriaRepository claimCriteriaRepository;
    private final S3FileStorage s3FileStorage;
    private final DocumentTransformer chunkingPipeline;
    private final VectorStore vectorStore;

    @Async("embeddingExecutor")
    public void process(Long claimCriteriaId) {
        try {
            com.swk.claimhelpers.policy.entity.Document documentEntity =
                    documentRepository.findByClaimCriteriaId(claimCriteriaId)
                            .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));

            byte[] pdfBytes = s3FileStorage.download(documentEntity.getObjectKey());
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            List<Document> documents = new PagePdfDocumentReader(resource,
                    PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(PdfDocumentReaderConfig.ALL_PAGES)
                            .build())
                    .read();
            List<Document> chunks = chunkingPipeline.apply(documents);

            chunks.forEach(chunk -> chunk.getMetadata().put(METADATA_CLAIM_CRITERIA_ID, claimCriteriaId));

            if (log.isTraceEnabled()) {
                for (int i = 0; i < chunks.size(); i++) {
                    String text = chunks.get(i).getText();
                    log.trace("청크[{}/{}] len={}, content={}",
                            i + 1, chunks.size(), text == null ? 0 : text.length(), text);
                }
            }

            long startedAt = System.currentTimeMillis();
            log.info("임베딩 시작: claimCriteriaId={}, 청크={}개", claimCriteriaId, chunks.size());
            vectorStore.add(chunks);
            log.info("임베딩 완료: claimCriteriaId={}, 소요={}ms", claimCriteriaId, System.currentTimeMillis() - startedAt);

            updateStatus(claimCriteriaId, ClaimCriteriaStatus.COMPLETED);
        } catch (Exception e) {
            log.error("약관 임베딩 처리 실패: claimCriteriaId={}", claimCriteriaId, e);
            updateStatus(claimCriteriaId, ClaimCriteriaStatus.FAILED);
        }
    }

    /**
     * 업로드 트랜잭션에서 PROCESSING 으로 커밋된 약관을 FAILED 로 되돌려 영구 PROCESSING 상태 방지.
     */
    public void markFailed(Long claimCriteriaId) {
        updateStatus(claimCriteriaId, ClaimCriteriaStatus.FAILED);
    }

    private void updateStatus(Long claimCriteriaId, ClaimCriteriaStatus status) {
        ClaimCriteria claimCriteria = claimCriteriaRepository.findById(claimCriteriaId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
        claimCriteria.updateStatus(status);
        claimCriteriaRepository.save(claimCriteria);
    }
}