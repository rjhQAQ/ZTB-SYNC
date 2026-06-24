package org.example.ztbsync.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.example.ztbsync.exception.BadRequestException;

/**
 * 测试文件下载接口请求参数。
 *
 * <p>projectId 同时兼容前端传入的 project_id 字段名。</p>
 */
public record DownloadFileTestRequest(
        @JsonAlias("project_id") String projectId,
        String fileId,
        String fileName) {

    public void validate() {
        if (isBlank(projectId)) {
            throw new BadRequestException("项目ID不能为空");
        }
        if (isBlank(fileId)) {
            throw new BadRequestException("文件ID不能为空");
        }
        if (isBlank(fileName)) {
            throw new BadRequestException("文件名称不能为空");
        }
    }

    public String normalizedProjectId() {
        return projectId.trim();
    }

    public String normalizedFileId() {
        return fileId.trim();
    }

    public String normalizedFileName() {
        return fileName.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
