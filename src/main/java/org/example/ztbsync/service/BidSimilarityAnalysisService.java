package org.example.ztbsync.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.BidSimilarityAnalysis;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.ProjectBidderCompany;
import org.example.ztbsync.domain.ProjectInfo;
import org.example.ztbsync.domain.SimilarityStatus;
import org.example.ztbsync.extraction.DocxTextExtractor;
import org.example.ztbsync.mapper.BidSimilarityAnalysisMapper;
import org.example.ztbsync.mapper.ProjectBidderCompanyMapper;
import org.example.ztbsync.mapper.ProjectInfoMapper;
import org.example.ztbsync.similarity.BidSimilarityCalculator;
import org.example.ztbsync.similarity.BidSimilarityLlmReviewClient;
import org.example.ztbsync.similarity.SimilarityComputationResult;
import org.example.ztbsync.similarity.SimilarityDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BidSimilarityAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(BidSimilarityAnalysisService.class);

    private final ZtbProperties properties;
    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectBidderCompanyMapper bidderCompanyMapper;
    private final BidSimilarityAnalysisMapper analysisMapper;
    private final FileDownloadClient fileDownloadClient;
    private final DocxTextExtractor docxTextExtractor;
    private final BidSimilarityCalculator calculator;
    private final BidSimilarityLlmReviewClient llmReviewClient;
    private final ObjectMapper objectMapper;

    public BidSimilarityAnalysisService(
            ZtbProperties properties,
            ProjectInfoMapper projectInfoMapper,
            ProjectBidderCompanyMapper bidderCompanyMapper,
            BidSimilarityAnalysisMapper analysisMapper,
            FileDownloadClient fileDownloadClient,
            DocxTextExtractor docxTextExtractor,
            BidSimilarityCalculator calculator,
            BidSimilarityLlmReviewClient llmReviewClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.projectInfoMapper = projectInfoMapper;
        this.bidderCompanyMapper = bidderCompanyMapper;
        this.analysisMapper = analysisMapper;
        this.fileDownloadClient = fileDownloadClient;
        this.docxTextExtractor = docxTextExtractor;
        this.calculator = calculator;
        this.llmReviewClient = llmReviewClient;
        this.objectMapper = objectMapper;
    }

    public void analyzeForCurrentBid(FileProcessingTask task, String currentCompanyName, String currentText) {
        if (!properties.getSimilarity().isEnabled()) {
            log.info("Bid similarity analysis disabled: taskId={}, projectId={}, fileId={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId());
            return;
        }
        List<ProjectBidderCompany> bidders = bidderCompanyMapper.findByProjectId(task.getProjectId());
        List<ProjectBidderCompany> others = bidders.stream()
                .filter(company -> !task.getFileId().equals(company.getFileId()))
                .toList();
        if (others.isEmpty()) {
            log.info("Skip bid similarity analysis because there is no peer bid file: taskId={}, projectId={}, fileId={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId());
            return;
        }

        List<ProjectInfo> tenders = projectInfoMapper.findByProjectId(task.getProjectId());
        ProjectInfo tender = tenders.isEmpty() ? null : tenders.get(0);
        String tenderText = null;
        String tenderError = null;
        if (tender == null) {
            tenderError = "项目缺少招标文件，无法执行招标来源降权";
        } else {
            try {
                tenderText = downloadText(tender.getFileId());
            } catch (Exception exception) {
                tenderError = rootMessage(exception);
            }
        }

        SimilarityDocument current = new SimilarityDocument(
                task.getFileId(),
                task.getFileName(),
                currentCompanyName,
                currentText);
        for (ProjectBidderCompany other : others) {
            if (tenderError != null) {
                upsertFailure(task, tender, current, fromCompany(other, null), tenderError);
                continue;
            }
            try {
                String otherText = downloadText(other.getFileId());
                SimilarityDocument peer = fromCompany(other, otherText);
                SimilarityPair pair = orderPair(current, peer);
                SimilarityComputationResult result = calculator.calculate(pair.left(), pair.right(), tenderText);
                String llmReviewJson = reviewWithLlm(pair, result);
                BidSimilarityAnalysis analysis = success(task, tender, pair, result, llmReviewJson);
                upsert(analysis);
                log.info("Saved bid similarity result: taskId={}, projectId={}, leftFileId={}, rightFileId={}, score={}, risk={}",
                        task.getTaskId(), task.getProjectId(), analysis.getLeftFileId(), analysis.getRightFileId(),
                        analysis.getScore(), analysis.getRiskLevel());
            } catch (Exception exception) {
                upsertFailure(task, tender, current, fromCompany(other, null), rootMessage(exception));
            }
        }
    }

    private String downloadText(String fileId) {
        byte[] bytes = fileDownloadClient.download(fileId);
        return docxTextExtractor.extract(bytes);
    }

    private String reviewWithLlm(SimilarityPair pair, SimilarityComputationResult result) {
        try {
            return llmReviewClient.review(pair.left(), pair.right(), result);
        } catch (Exception exception) {
            return "{\"reviewStatus\":\"FAILED\",\"reason\":\"" + escapeJson(rootMessage(exception)) + "\"}";
        }
    }

    private BidSimilarityAnalysis success(
            FileProcessingTask task,
            ProjectInfo tender,
            SimilarityPair pair,
            SimilarityComputationResult result,
            String llmReviewJson) {
        LocalDateTime now = LocalDateTime.now();
        BidSimilarityAnalysis analysis = base(task, tender, pair, now);
        analysis.setScore(result.score());
        analysis.setRiskLevel(result.riskLevel());
        analysis.setStatus(SimilarityStatus.SUCCESS.name());
        analysis.setHitFragmentsJson(writeJson(result.hitFragments()));
        analysis.setLlmReviewJson(llmReviewJson);
        analysis.setErrorMessage(null);
        return analysis;
    }

    private void upsertFailure(
            FileProcessingTask task,
            ProjectInfo tender,
            SimilarityDocument current,
            SimilarityDocument peer,
            String errorMessage) {
        SimilarityPair pair = orderPair(current, peer);
        LocalDateTime now = LocalDateTime.now();
        BidSimilarityAnalysis analysis = base(task, tender, pair, now);
        analysis.setScore(null);
        analysis.setRiskLevel(null);
        analysis.setStatus(SimilarityStatus.FAILED.name());
        analysis.setHitFragmentsJson(null);
        analysis.setLlmReviewJson(null);
        analysis.setErrorMessage(errorMessage);
        upsert(analysis);
        log.warn("Saved failed bid similarity result: taskId={}, projectId={}, leftFileId={}, rightFileId={}, message={}",
                task.getTaskId(), task.getProjectId(), analysis.getLeftFileId(), analysis.getRightFileId(), errorMessage);
    }

    private BidSimilarityAnalysis base(FileProcessingTask task, ProjectInfo tender, SimilarityPair pair, LocalDateTime now) {
        BidSimilarityAnalysis analysis = new BidSimilarityAnalysis();
        analysis.setProjectId(task.getProjectId());
        analysis.setTenderFileId(tender == null ? null : tender.getFileId());
        analysis.setTenderFileName(tender == null ? null : tender.getFileName());
        analysis.setLeftFileId(pair.left().fileId());
        analysis.setLeftFileName(pair.left().fileName());
        analysis.setLeftCompanyName(pair.left().companyName());
        analysis.setRightFileId(pair.right().fileId());
        analysis.setRightFileName(pair.right().fileName());
        analysis.setRightCompanyName(pair.right().companyName());
        analysis.setTaskId(task.getTaskId());
        analysis.setCreatedAt(now);
        analysis.setUpdatedAt(now);
        return analysis;
    }

    private void upsert(BidSimilarityAnalysis analysis) {
        if (analysisMapper.countByPair(
                analysis.getProjectId(),
                analysis.getLeftFileId(),
                analysis.getRightFileId()) > 0) {
            analysisMapper.updateByPair(analysis);
        } else {
            analysisMapper.insert(analysis);
        }
    }

    private SimilarityDocument fromCompany(ProjectBidderCompany company, String text) {
        return new SimilarityDocument(
                company.getFileId(),
                company.getFileName(),
                company.getBidCompanyName(),
                text);
    }

    private SimilarityPair orderPair(SimilarityDocument left, SimilarityDocument right) {
        return Comparator.comparing(SimilarityDocument::fileId).compare(left, right) <= 0
                ? new SimilarityPair(left, right)
                : new SimilarityPair(right, left);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("雷同分析结果 JSON 序列化失败", exception);
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

    private String escapeJson(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                        .replace("\"", "\\\"");
    }

    private record SimilarityPair(SimilarityDocument left, SimilarityDocument right) {
    }
}
