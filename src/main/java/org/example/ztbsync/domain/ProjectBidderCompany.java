package org.example.ztbsync.domain;

import java.time.LocalDateTime;

/**
 * 投标文件抽取后的投标企业信息实体，对应 ztb_project_bidder_company。
 */
public class ProjectBidderCompany {

    /** 数据库自增主键。 */
    private Long id;
    /** 项目 ID。 */
    private String projectId;
    /** 投标文件 ID。 */
    private String fileId;
    /** 投标文件名称。 */
    private String fileName;
    /** 投标公司名称。 */
    private String bidCompanyName;
    /** 投标人联系电话。 */
    private String bidderContactPhone;
    /** 注册地址。 */
    private String registeredAddress;
    /** 通信地址。 */
    private String mailingAddress;
    /** 项目管理人员信息 JSON。 */
    private String projectManagementPersonnelJson;
    /** 产生本次业务数据的任务 ID。 */
    private String taskId;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getBidCompanyName() {
        return bidCompanyName;
    }

    public void setBidCompanyName(String bidCompanyName) {
        this.bidCompanyName = bidCompanyName;
    }

    public String getBidderContactPhone() {
        return bidderContactPhone;
    }

    public void setBidderContactPhone(String bidderContactPhone) {
        this.bidderContactPhone = bidderContactPhone;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(String mailingAddress) {
        this.mailingAddress = mailingAddress;
    }

    public String getProjectManagementPersonnelJson() {
        return projectManagementPersonnelJson;
    }

    public void setProjectManagementPersonnelJson(String projectManagementPersonnelJson) {
        this.projectManagementPersonnelJson = projectManagementPersonnelJson;
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
