package org.example.ztbsync.domain;

import java.time.LocalDateTime;

/**
 * 投标文件两两雷同分析结果，对应 ztb_bid_similarity_analysis。
 */
public class BidSimilarityAnalysis {

    private String id;
    private String projectId;
    private String tenderFileId;
    private String tenderFileName;
    private String leftFileId;
    private String leftFileName;
    private String leftCompanyName;
    private String rightFileId;
    private String rightFileName;
    private String rightCompanyName;
    private Double score;
    private String riskLevel;
    private String status;
    private String hitFragmentsJson;
    private String llmReviewJson;
    private String errorMessage;
    private String taskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTenderFileId() {
        return tenderFileId;
    }

    public void setTenderFileId(String tenderFileId) {
        this.tenderFileId = tenderFileId;
    }

    public String getTenderFileName() {
        return tenderFileName;
    }

    public void setTenderFileName(String tenderFileName) {
        this.tenderFileName = tenderFileName;
    }

    public String getLeftFileId() {
        return leftFileId;
    }

    public void setLeftFileId(String leftFileId) {
        this.leftFileId = leftFileId;
    }

    public String getLeftFileName() {
        return leftFileName;
    }

    public void setLeftFileName(String leftFileName) {
        this.leftFileName = leftFileName;
    }

    public String getLeftCompanyName() {
        return leftCompanyName;
    }

    public void setLeftCompanyName(String leftCompanyName) {
        this.leftCompanyName = leftCompanyName;
    }

    public String getRightFileId() {
        return rightFileId;
    }

    public void setRightFileId(String rightFileId) {
        this.rightFileId = rightFileId;
    }

    public String getRightFileName() {
        return rightFileName;
    }

    public void setRightFileName(String rightFileName) {
        this.rightFileName = rightFileName;
    }

    public String getRightCompanyName() {
        return rightCompanyName;
    }

    public void setRightCompanyName(String rightCompanyName) {
        this.rightCompanyName = rightCompanyName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHitFragmentsJson() {
        return hitFragmentsJson;
    }

    public void setHitFragmentsJson(String hitFragmentsJson) {
        this.hitFragmentsJson = hitFragmentsJson;
    }

    public String getLlmReviewJson() {
        return llmReviewJson;
    }

    public void setLlmReviewJson(String llmReviewJson) {
        this.llmReviewJson = llmReviewJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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
}
