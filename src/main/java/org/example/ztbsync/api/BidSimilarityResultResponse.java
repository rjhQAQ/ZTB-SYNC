package org.example.ztbsync.api;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 投标文件两两雷同分析查询响应。
 */
public record BidSimilarityResultResponse(
        String projectId,
        String tenderFileId,
        String tenderFileName,
        String leftFileId,
        String leftFileName,
        String leftCompanyName,
        String rightFileId,
        String rightFileName,
        String rightCompanyName,
        Double score,
        String riskLevel,
        String status,
        JsonNode hitFragments,
        JsonNode llmReview,
        String errorMessage,
        String taskId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
