package org.example.ztbsync.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.example.ztbsync.domain.FileProcessingTask;
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
import org.junit.jupiter.api.Test;

class FileProcessingWorkerTest {

    @Test
    void latestTenderTaskDownloadsExtractsAndPersists() {
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        FileDownloadClient downloadClient = mock(FileDownloadClient.class);
        DocxTextExtractor docxTextExtractor = mock(DocxTextExtractor.class);
        TenderRegexExtractor tenderRegexExtractor = mock(TenderRegexExtractor.class);
        LlmExtractionClient llmExtractionClient = mock(LlmExtractionClient.class);
        ExtractionMerger merger = mock(ExtractionMerger.class);
        ExtractionPersistenceService persistenceService = mock(ExtractionPersistenceService.class);
        BidSimilarityAnalysisService similarityAnalysisService = mock(BidSimilarityAnalysisService.class);
        FileProcessingWorker worker = new FileProcessingWorker(
                taskMapper,
                downloadClient,
                docxTextExtractor,
                tenderRegexExtractor,
                mock(BidRegexExtractor.class),
                llmExtractionClient,
                merger,
                persistenceService,
                similarityAnalysisService);
        FileProcessingTask task = task("task-1", "TENDER");
        TenderExtraction regex = new TenderExtraction();
        TenderExtraction merged = new TenderExtraction();

        when(taskMapper.findById("task-1")).thenReturn(task);
        when(taskMapper.findByBusinessKey("project-1", "file-1", "TENDER")).thenReturn(List.of(task));
        when(downloadClient.download("file-1")).thenReturn(new byte[] {1, 2, 3});
        when(docxTextExtractor.extract(any())).thenReturn("项目名称：测试项目");
        when(llmExtractionClient.extract(any(), eq("项目名称：测试项目"))).thenReturn(LlmExtractionResult.empty());
        when(tenderRegexExtractor.extract("项目名称：测试项目")).thenReturn(regex);
        when(merger.mergeTender(eq(regex), any(TenderExtraction.class))).thenReturn(merged);

        worker.process("task-1");

        verify(taskMapper).markProcessing(eq("task-1"), any(LocalDateTime.class));
        verify(persistenceService).persistTenderAndComplete(task, merged);
        verify(similarityAnalysisService, never()).analyzeForCurrentBid(any(), any(), any());
    }

    @Test
    void latestBidTaskRunsSimilarityAfterPersistence() {
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        FileDownloadClient downloadClient = mock(FileDownloadClient.class);
        DocxTextExtractor docxTextExtractor = mock(DocxTextExtractor.class);
        BidRegexExtractor bidRegexExtractor = mock(BidRegexExtractor.class);
        LlmExtractionClient llmExtractionClient = mock(LlmExtractionClient.class);
        ExtractionMerger merger = mock(ExtractionMerger.class);
        ExtractionPersistenceService persistenceService = mock(ExtractionPersistenceService.class);
        BidSimilarityAnalysisService similarityAnalysisService = mock(BidSimilarityAnalysisService.class);
        FileProcessingWorker worker = new FileProcessingWorker(
                taskMapper,
                downloadClient,
                docxTextExtractor,
                mock(TenderRegexExtractor.class),
                bidRegexExtractor,
                llmExtractionClient,
                merger,
                persistenceService,
                similarityAnalysisService);
        FileProcessingTask task = task("task-1", "BID");
        BidExtraction regex = new BidExtraction();
        BidExtraction merged = new BidExtraction();
        merged.setBidCompanyName("甲公司");

        when(taskMapper.findById("task-1")).thenReturn(task);
        when(taskMapper.findByBusinessKey("project-1", "file-1", "BID")).thenReturn(List.of(task));
        when(downloadClient.download("file-1")).thenReturn(new byte[] {1, 2, 3});
        when(docxTextExtractor.extract(any())).thenReturn("投标人名称：甲公司");
        when(llmExtractionClient.extract(any(), eq("投标人名称：甲公司"))).thenReturn(LlmExtractionResult.empty());
        when(bidRegexExtractor.extract("投标人名称：甲公司")).thenReturn(regex);
        when(merger.mergeBid(eq(regex), any(BidExtraction.class))).thenReturn(merged);

        worker.process("task-1");

        verify(persistenceService).persistBidAndComplete(task, merged);
        verify(similarityAnalysisService).analyzeForCurrentBid(task, "甲公司", "投标人名称：甲公司");
    }

    @Test
    void nonLatestTaskIsSupersededWithoutDownloading() {
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        FileDownloadClient downloadClient = mock(FileDownloadClient.class);
        FileProcessingWorker worker = new FileProcessingWorker(
                taskMapper,
                downloadClient,
                mock(DocxTextExtractor.class),
                mock(TenderRegexExtractor.class),
                mock(BidRegexExtractor.class),
                mock(LlmExtractionClient.class),
                mock(ExtractionMerger.class),
                mock(ExtractionPersistenceService.class),
                mock(BidSimilarityAnalysisService.class));
        FileProcessingTask oldTask = task("old-task", "BID");
        FileProcessingTask newTask = task("new-task", "BID");

        when(taskMapper.findById("old-task")).thenReturn(oldTask);
        when(taskMapper.findByBusinessKey("project-1", "file-1", "BID")).thenReturn(List.of(newTask, oldTask));

        worker.process("old-task");

        verify(taskMapper).markSuperseded(eq("old-task"), any(LocalDateTime.class));
        verify(downloadClient, never()).download(any());
    }

    private FileProcessingTask task(String taskId, String type) {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId(taskId);
        task.setProjectId("project-1");
        task.setFileId("file-1");
        task.setFileName("文件.docx");
        task.setFileType(type);
        task.setStatus(ProcessingStatus.PENDING.name());
        return task;
    }
}
