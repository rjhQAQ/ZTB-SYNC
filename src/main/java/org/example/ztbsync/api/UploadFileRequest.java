package org.example.ztbsync.api;

/**
 * 文件处理任务创建请求。
 *
 * @param projectId 项目 ID，原样写入任务表和业务表
 * @param fileId 文件 ID，用于调用外部文件服务下载 DOCX 内容
 * @param fileName 文件名，v1 要求以 .docx 结尾
 * @param type 文件类型，支持 TENDER/BID/招标文件/投标文件
 */
public record UploadFileRequest(
        String projectId,
        String fileId,
        String fileName,
        String type) {
}
