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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * S3 에서 PDF 를 받아 Markdown 변환 → 청크 분할 → OpenAI 임베딩 → vector_store 저장까지 수행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimCriteriaProcessor {

    private final DocumentRepository documentRepository;
    private final ClaimCriteriaRepository claimCriteriaRepository;
    private final S3FileStorage s3FileStorage;
    private final PdfMarkdownExtractor pdfMarkdownExtractor;
    private final DocumentTransformer chunkingPipeline;
    private final VectorStore vectorStore;

    @Async("embeddingExecutor")
    public void process(Long claimCriteriaId) {
        Path workDir = null;
        try {
            com.swk.claimhelpers.policy.entity.Document documentEntity =
                    documentRepository.findByClaimCriteriaId(claimCriteriaId)
                            .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
            
            workDir = Files.createTempDirectory("claim-criteria-" + claimCriteriaId + "-");

            Path pdfFile = workDir.resolve("input.pdf");
            Path outputDir = Files.createDirectory(workDir.resolve("output"));

            s3FileStorage.downloadToFile(documentEntity.getObjectKey(), pdfFile);

            List<Document> documents = pdfMarkdownExtractor.extract(pdfFile, outputDir);

            List<Document> chunks = chunkingPipeline.apply(documents);
            chunks.forEach(chunk -> chunk.getMetadata().put(ClaimCriteriaVectorMetadata.CLAIM_CRITERIA_ID, claimCriteriaId));

            if(log.isTraceEnabled()) {
                for(int i = 0; i < chunks.size(); i++) {
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
        } finally {
            deleteTempDir(workDir);
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
    
    private void deleteTempDir(Path dir) {
        if(dir == null) {
            return;
        }
        try(java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch(IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", path, e);
                }
            });
        } catch(IOException e) {
            log.warn("임시 디렉터리 정리 실패: {}", dir, e);
        }
    }
}