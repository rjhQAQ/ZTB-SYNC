package org.example.ztbsync.service;

import java.time.LocalDateTime;
import java.util.List;

import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.domain.ProcessingStatus;
import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.BidRegexExtractor;
import org.example.ztbsync.extraction.DocxTextExtractor;
import org.example.ztbsync.extraction.ExtractionMerger;
import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.extraction.TenderRegexExtractor;
import org.example.ztbsync.llm.LlmExtractionClient;
import org.example.ztbsync.llm.LlmExtractionResult;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingWorker.class);

    private final FileProcessingTaskMapper taskMapper;
    private final FileDownloadClient fileDownloadClient;
    private final DocxTextExtractor docxTextExtractor;
    private final TenderRegexExtractor tenderRegexExtractor;
    private final BidRegexExtractor bidRegexExtractor;
    private final LlmExtractionClient llmExtractionClient;
    private final ExtractionMerger extractionMerger;
    private final ExtractionPersistenceService persistenceService;
    private final BidSimilarityAnalysisService bidSimilarityAnalysisService;

    public FileProcessingWorker(
            FileProcessingTaskMapper taskMapper,
            FileDownloadClient fileDownloadClient,
            DocxTextExtractor docxTextExtractor,
            TenderRegexExtractor tenderRegexExtractor,
            BidRegexExtractor bidRegexExtractor,
            LlmExtractionClient llmExtractionClient,
            ExtractionMerger extractionMerger,
            ExtractionPersistenceService persistenceService,
            BidSimilarityAnalysisService bidSimilarityAnalysisService) {
        this.taskMapper = taskMapper;
        this.fileDownloadClient = fileDownloadClient;
        this.docxTextExtractor = docxTextExtractor;
        this.tenderRegexExtractor = tenderRegexExtractor;
        this.bidRegexExtractor = bidRegexExtractor;
        this.llmExtractionClient = llmExtractionClient;
        this.extractionMerger = extractionMerger;
        this.persistenceService = persistenceService;
        this.bidSimilarityAnalysisService = bidSimilarityAnalysisService;
    }

    public void process(String taskId) {
        FileProcessingTask task = taskMapper.findById(taskId);
        if (task == null || ProcessingStatus.SUPERSEDED.name().equals(task.getStatus())) {
            log.info("Skip file processing task: taskId={}, reason={}", taskId,
                    task == null ? "task_not_found" : "already_superseded");
            return;
        }
        if (!isLatestTask(task)) {
            taskMapper.markSuperseded(taskId, LocalDateTime.now());
            log.info("Superseded stale file processing task before start: taskId={}, projectId={}, fileId={}, type={}",
                    taskId, task.getProjectId(), task.getFileId(), task.getFileType());
            return;
        }
        taskMapper.markProcessing(taskId, LocalDateTime.now());
        log.info("Started file processing task: taskId={}, projectId={}, fileId={}, fileName={}, type={}",
                taskId, task.getProjectId(), task.getFileId(), task.getFileName(), task.getFileType());

        try {
            byte[] bytes = fileDownloadClient.download(task.getFileId());
            String text = docxTextExtractor.extract(bytes);
            if (text.isBlank()) {
                throw new IllegalStateException("DOCX 未解析出有效文本");
            }
            log.info("Extracted DOCX text: taskId={}, fileId={}, bytes={}, textChars={}",
                    taskId, task.getFileId(), bytes.length, text.length());

            FileType fileType = FileType.from(task.getFileType());
            LlmExtractionResult llmResult = llmExtractionClient.extract(fileType, text);
            if (llmResult.rawJson() != null && !llmResult.rawJson().isBlank()) {
                taskMapper.updateLlmRawJson(taskId, llmResult.rawJson(), LocalDateTime.now());
                log.info("Stored LLM raw extraction JSON: taskId={}, rawJsonChars={}",
                        taskId, llmResult.rawJson().length());
            }
            if (!isLatestTask(task)) {
                taskMapper.markSuperseded(taskId, LocalDateTime.now());
                log.info("Superseded stale file processing task before persistence: taskId={}, projectId={}, fileId={}, type={}",
                        taskId, task.getProjectId(), task.getFileId(), task.getFileType());
                return;
            }

            // Regex results keep priority for explicit fields; LLM is used as a fallback for missing/complex fields.
            if (fileType == FileType.TENDER) {
                TenderExtraction regex = tenderRegexExtractor.extract(text);
                TenderExtraction merged = extractionMerger.mergeTender(regex, llmResult.tenderExtraction());
                persistenceService.persistTenderAndComplete(task, merged);
            } else {
                BidExtraction regex = bidRegexExtractor.extract(text);
                BidExtraction merged = extractionMerger.mergeBid(regex, llmResult.bidExtraction());
                persistenceService.persistBidAndComplete(task, merged);
                analyzeBidSimilarity(task, merged, text);
            }
            log.info("Finished file processing task successfully: taskId={}, projectId={}, fileId={}, type={}",
                    taskId, task.getProjectId(), task.getFileId(), task.getFileType());
        } catch (Exception exception) {
            log.error("File processing task failed: taskId={}, projectId={}, fileId={}, type={}, message={}",
                    taskId, task.getProjectId(), task.getFileId(), task.getFileType(), rootMessage(exception), exception);
            markFailedOrSuperseded(task, exception);
        }
    }

    private boolean isLatestTask(FileProcessingTask task) {
        List<FileProcessingTask> tasks = taskMapper.findByBusinessKey(
                task.getProjectId(),
                task.getFileId(),
                task.getFileType());
        return !tasks.isEmpty() && task.getTaskId().equals(tasks.get(0).getTaskId());
    }

    private void markFailedOrSuperseded(FileProcessingTask task, Exception exception) {
        if (!isLatestTask(task)) {
            taskMapper.markSuperseded(task.getTaskId(), LocalDateTime.now());
            log.info("Marked failed stale task as superseded: taskId={}, projectId={}, fileId={}, type={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), task.getFileType());
            return;
        }
        taskMapper.markFailed(task.getTaskId(), rootMessage(exception), LocalDateTime.now());
        log.info("Marked file processing task failed: taskId={}, projectId={}, fileId={}, type={}",
                task.getTaskId(), task.getProjectId(), task.getFileId(), task.getFileType());
    }

    private void analyzeBidSimilarity(FileProcessingTask task, BidExtraction extraction, String currentText) {
        try {
            bidSimilarityAnalysisService.analyzeForCurrentBid(
                    task,
                    extraction.getBidCompanyName(),
                    currentText);
        } catch (Exception exception) {
            log.error("Bid similarity analysis failed but bid extraction remains successful: taskId={}, projectId={}, fileId={}, message={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), rootMessage(exception), exception);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
