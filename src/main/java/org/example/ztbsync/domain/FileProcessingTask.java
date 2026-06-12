package org.example.ztbsync.domain;

import java.time.LocalDateTime;

/**
 * 文件异步处理任务实体，对应 ztb_file_processing_task。
 *
 * <p>任务表用于追踪上传受理、后台处理状态、LLM 原始结果和最终结果摘要。</p>
 */
public class FileProcessingTask {

    /** 后台处理任务 ID。 */
    private String taskId;
    /** 项目 ID，作为业务覆盖键的一部分。 */
    private String projectId;
    /** 文件 ID，用于下载文件内容，也是业务覆盖键的一部分。 */
    private String fileId;
    /** 原始文件名。 */
    private String fileName;
    /** 归一化后的文件类型，TENDER 或 BID。 */
    private String fileType;
    /** 任务状态，取值见 ProcessingStatus。 */
    private String status;
    /** 失败时记录的错误摘要。 */
    private String errorMessage;
    /** LLM 返回的原始 JSON，便于排查抽取效果。 */
    private String llmRawJson;
    /** 成功后写入的最终抽取结果摘要 JSON。 */
    private String resultSummaryJson;
    /** 任务创建时间。 */
    private LocalDateTime createdAt;
    /** 任务最后更新时间。 */
    private LocalDateTime updatedAt;
    /** 后台开始处理时间。 */
    private LocalDateTime startedAt;
    /** 后台处理结束时间。 */
    private LocalDateTime finishedAt;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getLlmRawJson() {
        return llmRawJson;
    }

    public void setLlmRawJson(String llmRawJson) {
        this.llmRawJson = llmRawJson;
    }

    public String getResultSummaryJson() {
        return resultSummaryJson;
    }

    public void setResultSummaryJson(String resultSummaryJson) {
        this.resultSummaryJson = resultSummaryJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
