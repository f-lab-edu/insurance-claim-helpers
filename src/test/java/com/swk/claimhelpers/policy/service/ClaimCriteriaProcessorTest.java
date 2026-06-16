package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.common.storage.S3FileStorage;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.ClaimCriteriaRepository;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClaimCriteriaProcessorTest {

    private static final Long CLAIM_CRITERIA_ID = 1L;
    private static final String OBJECT_KEY = "claim-criteria/1/file.pdf";

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClaimCriteriaRepository claimCriteriaRepository;

    @Mock
    private S3FileStorage s3FileStorage;

    @Mock
    private DocumentTransformer chunkingPipeline;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private ClaimCriteriaProcessor processor;

    @Test
    @DisplayName("정상 처리: 청크에 claim_criteria_id 주입 + vectorStore.add 호출 + 상태 COMPLETED")
    void 정상_처리() throws IOException {
        ClaimCriteria claimCriteria = ClaimCriteria.createForSession("session-1");
        Document documentEntity = Document.create(claimCriteria, "약관.pdf", 100L);
        documentEntity.updateObjectKey(OBJECT_KEY);
        given(documentRepository.findByClaimCriteriaId(CLAIM_CRITERIA_ID)).willReturn(Optional.of(documentEntity));
        given(s3FileStorage.download(OBJECT_KEY)).willReturn(minimalPdfBytes());

        // 실제 PagePdfDocumentReader 추출 결과는 mock 파이프라인이 받아 우리가 만든 청크로 치환한다.
        org.springframework.ai.document.Document chunk = new org.springframework.ai.document.Document("제1조 ...");
        given(chunkingPipeline.apply(anyList())).willReturn(List.of(chunk));
        given(claimCriteriaRepository.findById(CLAIM_CRITERIA_ID)).willReturn(Optional.of(claimCriteria));

        processor.process(CLAIM_CRITERIA_ID);

        ArgumentCaptor<List<org.springframework.ai.document.Document>> captor = ArgumentCaptor.captor();
        then(vectorStore).should().add(captor.capture());
        List<org.springframework.ai.document.Document> added = captor.getValue();
        assertThat(added).hasSize(1);
        assertThat(added.get(0).getText()).isEqualTo("제1조 ...");
        assertThat(added.get(0).getMetadata()).containsEntry("claim_criteria_id", CLAIM_CRITERIA_ID);
        assertThat(claimCriteria.getStatus()).isEqualTo(ClaimCriteriaStatus.COMPLETED);
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 상태를 FAILED 로 전환하고 vectorStore 에 저장하지 않는다")
    void 예외_발생_시_FAILED() {
        ClaimCriteria claimCriteria = ClaimCriteria.createForSession("session-1");
        Document documentEntity = Document.create(claimCriteria, "약관.pdf", 100L);
        documentEntity.updateObjectKey(OBJECT_KEY);
        given(documentRepository.findByClaimCriteriaId(CLAIM_CRITERIA_ID)).willReturn(Optional.of(documentEntity));
        // S3 다운로드 단계에서 실패하도록 스텁
        given(s3FileStorage.download(OBJECT_KEY)).willThrow(new RuntimeException("download failed"));
        given(claimCriteriaRepository.findById(CLAIM_CRITERIA_ID)).willReturn(Optional.of(claimCriteria));

        processor.process(CLAIM_CRITERIA_ID);

        assertThat(claimCriteria.getStatus()).isEqualTo(ClaimCriteriaStatus.FAILED);
        then(vectorStore).should(never()).add(any());
    }

    // PagePdfDocumentReader 가 파싱할 수 있는 최소 PDF(텍스트 1줄, 1페이지)를 메모리에서 생성
    private byte[] minimalPdfBytes() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("clause text");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}