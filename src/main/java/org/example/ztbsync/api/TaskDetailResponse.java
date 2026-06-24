package org.example.ztbsync.api;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 任务详情查询响应。
 *
 * @param resultSummary 成功时为抽取结果摘要 JSON；失败或未完成时为空
 * @param embeddingStatus RAG 向量入库状态；默认关闭时为 SKIPPED
 * @param embeddingErrorMessage embedding 或 ES 写入失败时的错误摘要
 * @param embeddingIndexedSegments 已写入 ES 的 chunk 数量
 * @param startedAt 后台任务开始处理时间
 * @param finishedAt 后台任务结束时间，成功、失败或被覆盖时写入
 */
public record TaskDetailResponse(
        String taskId,
        String projectId,
        String fileId,
        String fileName,
        String type,
        String status,
        String errorMessage,
        JsonNode resultSummary,
        String embeddingStatus,
        String embeddingErrorMessage,
        Integer embeddingIndexedSegments,
        LocalDateTime embeddingUpdatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
