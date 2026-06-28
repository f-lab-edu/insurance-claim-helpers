package com.swk.claimhelpers.policy.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.opendataloader.pdf.api.Config;
import org.opendataloader.pdf.api.OpenDataLoaderPDF;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class PdfMarkdownExtractor {

    /**
     * @param pdfFile   변환할 PDF 경로
     * @param outputDir opendataloader 가 .md 를 쓸 디렉터리
     * @return Document 리스트
     */
    public List<Document> extract(Path pdfFile, Path outputDir) {
        try {
            Config config = new Config();
            config.setOutputFolder(outputDir.toString());
            config.setGenerateMarkdown(true);
            config.setGeneratePDF(false);
            config.setGenerateHtml(false);
            config.setGenerateJSON(false);

            OpenDataLoaderPDF.processFile(pdfFile.toString(), config);

            Path markdown = outputDir.resolve(toMarkdownFileName(pdfFile));
            if(!Files.exists(markdown)) {
                log.error("변환 결과 .md 를 찾을 수 없음: expected={}", markdown);
                throw new CustomException(ErrorCode.INTERNAL_ERROR);
            }

            MarkdownDocumentReaderConfig readerConfig = MarkdownDocumentReaderConfig.builder().build();
            return new MarkdownDocumentReader(new FileSystemResource(markdown.toFile()), readerConfig).read();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF→Markdown 변환 실패: pdf={}", pdfFile, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, e);
        }
    }
    
    private static String toMarkdownFileName(Path pdfFile) {
        String name = pdfFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0) ? name.substring(0, dot) : name;
        return base + ".md";
    }
}