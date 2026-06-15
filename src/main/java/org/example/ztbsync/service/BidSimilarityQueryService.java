package org.example.ztbsync.service;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.example.ztbsync.api.BidSimilarityResultResponse;
import org.example.ztbsync.domain.BidSimilarityAnalysis;
import org.example.ztbsync.mapper.BidSimilarityAnalysisMapper;
import org.springframework.stereotype.Service;

@Service
public class BidSimilarityQueryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final BidSimilarityAnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    public BidSimilarityQueryService(BidSimilarityAnalysisMapper analysisMapper, ObjectMapper objectMapper) {
        this.analysisMapper = analysisMapper;
        this.objectMapper = objectMapper;
    }

    public List<BidSimilarityResultResponse> findByProject(
            String projectId,
            Double minScore,
            String riskLevel,
            String status,
            Integer limit,
            Integer offset) {
        return analysisMapper.findByProject(
                        projectId,
                        minScore,
                        normalize(riskLevel),
                        normalize(status),
                        safeLimit(limit),
                        safeOffset(offset))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BidSimilarityResultResponse> findByProjectAndFile(
            String projectId,
            String fileId,
            Double minScore,
            String riskLevel,
            String status,
            Integer limit,
            Integer offset) {
        return analysisMapper.findByProjectAndFile(
                        projectId,
                        fileId,
                        minScore,
                        normalize(riskLevel),
                        normalize(status),
                        safeLimit(limit),
                        safeOffset(offset))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BidSimilarityResultResponse toResponse(BidSimilarityAnalysis analysis) {
        return new BidSimilarityResultResponse(
                analysis.getProjectId(),
                analysis.getTenderFileId(),
                analysis.getTenderFileName(),
                analysis.getLeftFileId(),
                analysis.getLeftFileName(),
                analysis.getLeftCompanyName(),
                analysis.getRightFileId(),
                analysis.getRightFileName(),
                analysis.getRightCompanyName(),
                analysis.getScore(),
                analysis.getRiskLevel(),
                analysis.getStatus(),
                parseJson(analysis.getHitFragmentsJson()),
                parseJson(analysis.getLlmReviewJson()),
                analysis.getErrorMessage(),
                analysis.getTaskId(),
                analysis.getCreatedAt(),
                analysis.getUpdatedAt());
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return TextNode.valueOf(value);
        }
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int safeOffset(Integer offset) {
        return offset == null || offset < 0 ? 0 : offset;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
