package org.example.ztbsync.api;

/**
 * 直接上传文件做 embedding 入库的测试响应。
 *
 * @param taskId 本次测试写入 ES 时使用的临时任务 ID
 * @param indexedSegments 写入 ES 的 chunk 数量
 */
public record EmbeddingTestResponse(
        String taskId,
        String projectId,
        String fileId,
        String fileName,
        String type,
        long fileSize,
        int textChars,
        int blockCount,
        int indexedSegments,
        String indexName,
        String model,
        int vectorDims) {
}
