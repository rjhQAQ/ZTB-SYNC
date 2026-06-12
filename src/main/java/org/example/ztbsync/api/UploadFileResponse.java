package org.example.ztbsync.api;

/**
 * 文件处理任务创建后的受理响应。
 *
 * @param taskId 后台异步任务 ID
 * @param status 初始状态，通常为 PENDING
 * @param projectId 项目 ID
 * @param fileId 文件 ID
 * @param fileName 文件名
 * @param type 归一化后的文件类型，TENDER 或 BID
 */
public record UploadFileResponse(
        String taskId,
        String status,
        String projectId,
        String fileId,
        String fileName,
        String type) {
}
