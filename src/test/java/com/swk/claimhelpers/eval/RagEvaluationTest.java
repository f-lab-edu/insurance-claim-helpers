package com.swk.claimhelpers.eval;

import com.swk.claimhelpers.chat.dto.ChatContext;
import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.MessageRole;
import com.swk.claimhelpers.chat.service.ChatMessageService;
import com.swk.claimhelpers.chat.service.ChatPromptBuilder;
import com.swk.claimhelpers.policy.service.ClaimCriteriaVectorMetadata;
import com.swk.claimhelpers.policy.service.PdfMarkdownExtractor;
import com.swk.claimhelpers.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("eval")
@SpringBootTest
@Import(EvalJudgeConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CLIENT_SECRET", matches = ".+")
class RagEvaluationTest extends AbstractPostgresContainerTest {

    private static final int N = 5;                       // 반복 횟수
    private static final long EVAL_CRITERIA_ID = 999_999L; // 평가용 논리 약관 id
    private static final double CONTEXT_PRECISION_PASS = 0.5;
    private static final String EVAL_PDF =
            "/2026-06-18_프로미카다이렉트TM개인용약관-보통약관_분할.pdf";

    @Autowired private PdfMarkdownExtractor pdfMarkdownExtractor;
    @Autowired private DocumentTransformer chunkingPipeline;
    @Autowired private VectorStore vectorStore;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private ChatPromptBuilder chatPromptBuilder;
    @Autowired private ChatClient chatClient;                 // 생성용(Sonnet)
    @Autowired private FactCheckingEvaluator factCheckingEvaluator;
    @Autowired private RelevancyEvaluator relevancyEvaluator;
    @Autowired private ContextRelevanceJudge contextRelevanceJudge;
    @Autowired private NegativeRejectionJudge negativeRejectionJudge;

    private final EvalReport report = new EvalReport();

    @BeforeAll
    void 약관_임베딩_적재() throws Exception {
        Path workDir = Files.createTempDirectory("eval-");
        Path pdf = workDir.resolve("input.pdf");
        Path out = Files.createDirectory(workDir.resolve("output"));
        try(InputStream in = getClass().getResourceAsStream(EVAL_PDF)) {
            assertThat(in).as("평가 PDF 리소스").isNotNull();
            Files.copy(in, pdf);
        }
        List<Document> documents = pdfMarkdownExtractor.extract(pdf, out);
        List<Document> chunks = chunkingPipeline.apply(documents);
        chunks.forEach(c -> c.getMetadata().put(ClaimCriteriaVectorMetadata.CLAIM_CRITERIA_ID, EVAL_CRITERIA_ID));
        vectorStore.add(chunks);   // 실 OpenAI 임베딩
    }

    static List<EvalQuestion> questions() {
        return new EvalQuestionLoader().load();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("questions")
    void 질문별_반복_평가(EvalQuestion q) {
        int faith = 0;
        int answerRel = 0;
        int contextRel = 0;
        double precisionSum = 0.0;
        int rejection = 0;

        for(int run = 0; run < N; run++) {
            ChatMessage userMsg = ChatMessage.create(null, MessageRole.USER, q.question());
            ChatContext context = new ChatContext(List.of(EVAL_CRITERIA_ID), List.of(userMsg));

            List<Document> chunks = chatMessageService.retrieve(q.question(), context);

            if(!q.answerable()) {
                if(chunks.isEmpty()) {
                    rejection++;
                } else {
                    String answer = generate(chunks, userMsg);
                    if(negativeRejectionJudge.evaluate(q.question(), answer)) {
                        rejection++;
                    }
                }
                continue;
            }

            // 답변형: 청크가 없으면 이번 회차는 모든 생성 지표 실패로 간주(검색 미스)
            if(chunks.isEmpty()) {
                continue;
            }
            String answer = generate(chunks, userMsg);

            // 1) Faithfulness — 답변이 청크(document)에 지지되나
            if(factCheckingEvaluator.evaluate(new EvaluationRequest(chunks, answer)).isPass()) {
                faith++;
            }
            // 2) Answer Relevance — 답변이 질문+청크에 관련 있나
            if(relevancyEvaluator.evaluate(new EvaluationRequest(q.question(), chunks, answer)).isPass()) {
                answerRel++;
            }
            // 3) Context Relevance — 검색된 청크의 관련 비율(Precision@k)
            double precision = contextRelevanceJudge.evaluate(q.question(), chunks);
            precisionSum += precision;
            if(precision >= CONTEXT_PRECISION_PASS) {
                contextRel++;
            }
        }

        report.add(new QuestionOutcome(
                q.id(), q.answerable(), N, faith, answerRel, contextRel, precisionSum, rejection));

        assertThat(report.format()).contains(q.id());
    }

    private String generate(List<Document> chunks, ChatMessage userMsg) {
        List<Message> messages = chatPromptBuilder.build(chunks, List.of(userMsg));
        return chatClient.prompt().messages(messages).call().content();
    }

    @AfterAll
    void 리포트_출력() {
        System.out.println(report.format());
    }
}