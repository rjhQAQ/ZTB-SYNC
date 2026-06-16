package org.example.ztbsync.domain;

import java.time.LocalDateTime;

/**
 * 招标文件抽取后的项目信息实体，对应 ztb_project_info。
 */
public class ProjectInfo {

    /** UUID 主键。 */
    private String id;
    /** 项目 ID。 */
    private String projectId;
    /** 招标文件 ID。 */
    private String fileId;
    /** 招标文件名称。 */
    private String fileName;
    /** 招标企业或招标人名称。 */
    private String tenderCompanyName;
    /** 代理机构名称。 */
    private String agencyName;
    /** 项目名称。 */
    private String projectName;
    /** 投标文件递交开始时间。 */
    private LocalDateTime bidSubmitStartTime;
    /** 投标文件递交结束或截止时间。 */
    private LocalDateTime bidSubmitEndTime;
    /** 项目相关所有时间点中的最早时间。 */
    private LocalDateTime rangeStartTime;
    /** 项目相关所有时间点中的最晚时间。 */
    private LocalDateTime rangeEndTime;
    /** 项目相关时间点明细 JSON。 */
    private String allTimePointsJson;
    /** 产生本次业务数据的任务 ID。 */
    private String taskId;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
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

    public String getTenderCompanyName() {
        return tenderCompanyName;
    }

    public void setTenderCompanyName(String tenderCompanyName) {
        this.tenderCompanyName = tenderCompanyName;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public LocalDateTime getBidSubmitStartTime() {
        return bidSubmitStartTime;
    }

    public void setBidSubmitStartTime(LocalDateTime bidSubmitStartTime) {
        this.bidSubmitStartTime = bidSubmitStartTime;
    }

    public LocalDateTime getBidSubmitEndTime() {
        return bidSubmitEndTime;
    }

    public void setBidSubmitEndTime(LocalDateTime bidSubmitEndTime) {
        this.bidSubmitEndTime = bidSubmitEndTime;
    }

    public LocalDateTime getRangeStartTime() {
        return rangeStartTime;
    }

    public void setRangeStartTime(LocalDateTime rangeStartTime) {
        this.rangeStartTime = rangeStartTime;
    }

    public LocalDateTime getRangeEndTime() {
        return rangeEndTime;
    }

    public void setRangeEndTime(LocalDateTime rangeEndTime) {
        this.rangeEndTime = rangeEndTime;
    }

    public String getAllTimePointsJson() {
        return allTimePointsJson;
    }

    public void setAllTimePointsJson(String allTimePointsJson) {
        this.allTimePointsJson = allTimePointsJson;
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
