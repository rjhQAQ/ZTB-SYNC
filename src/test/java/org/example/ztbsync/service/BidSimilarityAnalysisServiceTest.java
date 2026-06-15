package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.BidSimilarityAnalysis;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.ProjectBidderCompany;
import org.example.ztbsync.domain.ProjectInfo;
import org.example.ztbsync.extraction.DocxTextExtractor;
import org.example.ztbsync.mapper.BidSimilarityAnalysisMapper;
import org.example.ztbsync.mapper.ProjectBidderCompanyMapper;
import org.example.ztbsync.mapper.ProjectInfoMapper;
import org.example.ztbsync.similarity.BidSimilarityCalculator;
import org.example.ztbsync.similarity.BidSimilarityLlmReviewClient;
import org.example.ztbsync.similarity.HitFragment;
import org.example.ztbsync.similarity.SimilarityComputationResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BidSimilarityAnalysisServiceTest {

    @Test
    void downloadsTenderAndPeerFileThenPersistsSuccessfulPair() {
        ZtbProperties properties = new ZtbProperties();
        ProjectInfoMapper projectInfoMapper = mock(ProjectInfoMapper.class);
        ProjectBidderCompanyMapper bidderMapper = mock(ProjectBidderCompanyMapper.class);
        BidSimilarityAnalysisMapper analysisMapper = mock(BidSimilarityAnalysisMapper.class);
        FileDownloadClient downloadClient = mock(FileDownloadClient.class);
        DocxTextExtractor docxTextExtractor = mock(DocxTextExtractor.class);
        BidSimilarityCalculator calculator = mock(BidSimilarityCalculator.class);
        BidSimilarityLlmReviewClient llmReviewClient = mock(BidSimilarityLlmReviewClient.class);
        BidSimilarityAnalysisService service = new BidSimilarityAnalysisService(
                properties,
                projectInfoMapper,
                bidderMapper,
                analysisMapper,
                downloadClient,
                docxTextExtractor,
                calculator,
                llmReviewClient,
                new ObjectMapper());
        FileProcessingTask task = task();
        ProjectInfo tender = tender();
        ProjectBidderCompany peer = bidder("peer-file", "乙公司");
        SimilarityComputationResult result = new SimilarityComputationResult(
                88.5,
                "HIGH",
                0.8,
                0.9,
                5,
                List.of(new HitFragment("TEXT_SIMILARITY", "NON_STANDARD_TEXT", "左", "右", 0.9, false, 0, 1)));

        when(projectInfoMapper.findByProjectId("project-1")).thenReturn(List.of(tender));
        when(bidderMapper.findByProjectId("project-1")).thenReturn(List.of(peer, bidder("current-file", "甲公司")));
        when(downloadClient.download("tender-file")).thenReturn(new byte[] {1});
        when(downloadClient.download("peer-file")).thenReturn(new byte[] {2});
        when(docxTextExtractor.extract(new byte[] {1})).thenReturn("招标文本");
        when(docxTextExtractor.extract(new byte[] {2})).thenReturn("历史投标文本");
        when(calculator.calculate(any(), any(), eq("招标文本"))).thenReturn(result);
        when(llmReviewClient.review(any(), any(), eq(result))).thenReturn(null);

        service.analyzeForCurrentBid(task, "甲公司", "当前投标文本");

        ArgumentCaptor<BidSimilarityAnalysis> captor = ArgumentCaptor.forClass(BidSimilarityAnalysis.class);
        verify(analysisMapper).insert(captor.capture());
        BidSimilarityAnalysis saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getTenderFileId()).isEqualTo("tender-file");
        assertThat(saved.getLeftFileId()).isEqualTo("current-file");
        assertThat(saved.getRightFileId()).isEqualTo("peer-file");
        assertThat(saved.getScore()).isEqualTo(88.5);
    }

    @Test
    void writesFailedPairWhenPeerFileCannotDownload() {
        ZtbProperties properties = new ZtbProperties();
        ProjectInfoMapper projectInfoMapper = mock(ProjectInfoMapper.class);
        ProjectBidderCompanyMapper bidderMapper = mock(ProjectBidderCompanyMapper.class);
        BidSimilarityAnalysisMapper analysisMapper = mock(BidSimilarityAnalysisMapper.class);
        FileDownloadClient downloadClient = mock(FileDownloadClient.class);
        DocxTextExtractor docxTextExtractor = mock(DocxTextExtractor.class);
        BidSimilarityAnalysisService service = new BidSimilarityAnalysisService(
                properties,
                projectInfoMapper,
                bidderMapper,
                analysisMapper,
                downloadClient,
                docxTextExtractor,
                mock(BidSimilarityCalculator.class),
                mock(BidSimilarityLlmReviewClient.class),
                new ObjectMapper());

        when(projectInfoMapper.findByProjectId("project-1")).thenReturn(List.of(tender()));
        when(bidderMapper.findByProjectId("project-1")).thenReturn(List.of(bidder("peer-file", "乙公司")));
        when(downloadClient.download("tender-file")).thenReturn(new byte[] {1});
        when(docxTextExtractor.extract(new byte[] {1})).thenReturn("招标文本");
        when(downloadClient.download("peer-file")).thenThrow(new IllegalStateException("download failed"));

        service.analyzeForCurrentBid(task(), "甲公司", "当前投标文本");

        ArgumentCaptor<BidSimilarityAnalysis> captor = ArgumentCaptor.forClass(BidSimilarityAnalysis.class);
        verify(analysisMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("download failed");
    }

    private FileProcessingTask task() {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId("task-1");
        task.setProjectId("project-1");
        task.setFileId("current-file");
        task.setFileName("当前.docx");
        return task;
    }

    private ProjectInfo tender() {
        ProjectInfo tender = new ProjectInfo();
        tender.setProjectId("project-1");
        tender.setFileId("tender-file");
        tender.setFileName("招标.docx");
        return tender;
    }

    private ProjectBidderCompany bidder(String fileId, String companyName) {
        ProjectBidderCompany company = new ProjectBidderCompany();
        company.setProjectId("project-1");
        company.setFileId(fileId);
        company.setFileName(fileId + ".docx");
        company.setBidCompanyName(companyName);
        return company;
    }
}
