package org.example.ztbsync.api;

import org.example.ztbsync.service.FileTaskQueryService;
import org.example.ztbsync.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件上传和异步抽取任务查询接口。
 *
 * <p>这里的“上传”只接收文件元数据，真实文件内容由后台根据 fileId 调文件服务下载。</p>
 */
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService uploadService;
    private final FileTaskQueryService queryService;

    public FileUploadController(FileUploadService uploadService, FileTaskQueryService queryService) {
        this.uploadService = uploadService;
        this.queryService = queryService;
    }

    /**
     * 创建文件处理任务，并在事务提交后异步下载、解析、抽取和落库。
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> upload(@RequestBody UploadFileRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(uploadService.submit(request));
    }

    /**
     * 根据任务 ID 查询处理状态、错误信息和抽取结果摘要。
     */
    @GetMapping("/tasks/{taskId}")
    public TaskDetailResponse getTask(@PathVariable String taskId) {
        return queryService.getTask(taskId);
    }

    /**
     * 查询同一个 projectId + fileId + type 下最新的一条处理任务。
     */
    @GetMapping("/tasks/latest")
    public TaskDetailResponse getLatestTask(
            @RequestParam String projectId,
            @RequestParam String fileId,
            @RequestParam String type) {
        return queryService.getLatestTask(projectId, fileId, type);
    }
}
