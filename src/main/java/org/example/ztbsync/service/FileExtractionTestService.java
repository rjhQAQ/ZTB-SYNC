package org.example.ztbsync.service;

import java.io.IOException;
import java.util.Locale;

import org.example.ztbsync.api.ExtractionTestResponse;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.BidRegexExtractor;
import org.example.ztbsync.extraction.DocxTextExtractor;
import org.example.ztbsync.extraction.ExtractionMerger;
import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.extraction.TenderRegexExtractor;
import org.example.ztbsync.llm.LlmExtractionClient;
import org.example.ztbsync.llm.LlmExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileExtractionTestService {

    private static final Logger log = LoggerFactory.getLogger(FileExtractionTestService.class);

    private final DocxTextExtractor docxTextExtractor;
    private final TenderRegexExtractor tenderRegexExtractor;
    private final BidRegexExtractor bidRegexExtractor;
    private final LlmExtractionClient llmExtractionClient;
    private final ExtractionMerger extractionMerger;

    public FileExtractionTestService(
            DocxTextExtractor docxTextExtractor,
            TenderRegexExtractor tenderRegexExtractor,
            BidRegexExtractor bidRegexExtractor,
            LlmExtractionClient llmExtractionClient,
            ExtractionMerger extractionMerger) {
        this.docxTextExtractor = docxTextExtractor;
        this.tenderRegexExtractor = tenderRegexExtractor;
        this.bidRegexExtractor = bidRegexExtractor;
        this.llmExtractionClient = llmExtractionClient;
        this.extractionMerger = extractionMerger;
    }

    /**
     * 直接解析上传文件并返回抽取结果，主要用于联调抽取规则和 LLM 提示词。
     */
    public ExtractionTestResponse extract(MultipartFile file, String typeValue) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new BadRequestException("测试抽取接口仅支持 DOCX 文件");
        }

        FileType fileType = FileType.from(typeValue);
        byte[] bytes = readBytes(file);
        String text = docxTextExtractor.extract(bytes);
        if (text.isBlank()) {
            throw new BadRequestException("DOCX 未解析出有效文本");
        }
        log.info("Started test extraction: fileName={}, type={}, bytes={}, textChars={}",
                fileName, fileType, bytes.length, text.length());

        LlmExtractionResult llmResult = llmExtractionClient.extract(fileType, text);
        ExtractionTestResponse response = fileType == FileType.TENDER
                ? extractTender(fileName, bytes.length, text.length(), llmResult, text)
                : extractBid(fileName, bytes.length, text.length(), llmResult, text);
        log.info("Finished test extraction: fileName={}, type={}, textChars={}, hasLlmRaw={}",
                fileName, fileType, text.length(), llmResult.rawJson() != null && !llmResult.rawJson().isBlank());
        return response;
    }

    private ExtractionTestResponse extractTender(
            String fileName,
            int fileSize,
            int textChars,
            LlmExtractionResult llmResult,
            String text) {
        TenderExtraction regex = tenderRegexExtractor.extract(text);
        TenderExtraction llm = llmResult.tenderExtraction();
        TenderExtraction merged = extractionMerger.mergeTender(regex, llm);
        return new ExtractionTestResponse(
                fileName,
                FileType.TENDER.code(),
                fileSize,
                textChars,
                llmResult.rawJson(),
                regex,
                llm,
                merged);
    }

    private ExtractionTestResponse extractBid(
            String fileName,
            int fileSize,
            int textChars,
            LlmExtractionResult llmResult,
            String text) {
        BidExtraction regex = bidRegexExtractor.extract(text);
        BidExtraction llm = llmResult.bidExtraction();
        BidExtraction merged = extractionMerger.mergeBid(regex, llm);
        return new ExtractionTestResponse(
                fileName,
                FileType.BID.code(),
                fileSize,
                textChars,
                llmResult.rawJson(),
                regex,
                llm,
                merged);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("读取上传文件失败");
        }
    }
}
