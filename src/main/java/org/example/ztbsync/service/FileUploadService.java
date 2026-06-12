package org.example.ztbsync.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.example.ztbsync.api.UploadFileRequest;
import org.example.ztbsync.api.UploadFileResponse;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.domain.ProcessingStatus;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final FileProcessingTaskMapper taskMapper;
    private final FileProcessingWorker worker;
    private final TaskExecutor fileProcessingExecutor;

    public FileUploadService(
            FileProcessingTaskMapper taskMapper,
            FileProcessingWorker worker,
            @Qualifier("fileProcessingExecutor") TaskExecutor fileProcessingExecutor) {
        this.taskMapper = taskMapper;
        this.worker = worker;
        this.fileProcessingExecutor = fileProcessingExecutor;
    }

    @Transactional
    public UploadFileResponse submit(UploadFileRequest request) {
        validate(request);
        FileType fileType = FileType.from(request.type());
        LocalDateTime now = LocalDateTime.now();
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setProjectId(request.projectId().trim());
        task.setFileId(request.fileId().trim());
        task.setFileName(request.fileName().trim());
        task.setFileType(fileType.code());
        task.setStatus(ProcessingStatus.PENDING.name());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskMapper.insert(task);
        int supersededCount = taskMapper.markOlderActiveTasksSuperseded(
                task.getProjectId(),
                task.getFileId(),
                task.getFileType(),
                task.getTaskId(),
                now);
        scheduleAfterCommit(task.getTaskId());
        log.info("Accepted file processing task: taskId={}, projectId={}, fileId={}, fileName={}, type={}, supersededActiveTasks={}",
                task.getTaskId(), task.getProjectId(), task.getFileId(), task.getFileName(),
                task.getFileType(), supersededCount);

        return new UploadFileResponse(
                task.getTaskId(),
                task.getStatus(),
                task.getProjectId(),
                task.getFileId(),
                task.getFileName(),
                task.getFileType());
    }

    private void validate(UploadFileRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        requireText(request.projectId(), "projectId 不能为空");
        requireText(request.fileId(), "fileId 不能为空");
        requireText(request.fileName(), "fileName 不能为空");
        requireText(request.type(), "type 不能为空");
        String lowerName = request.fileName().trim().toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".docx")) {
            throw new BadRequestException("v1 仅支持 DOCX 文件");
        }
        try {
            FileType.from(request.type());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void scheduleAfterCommit(String taskId) {
        Runnable task = () -> fileProcessingExecutor.execute(() -> worker.process(taskId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // The background thread must start after the INSERT commits, otherwise it may not see the task row.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Scheduling file processing task after transaction commit: taskId={}", taskId);
                    task.run();
                }
            });
        } else {
            log.debug("Scheduling file processing task immediately: taskId={}", taskId);
            task.run();
        }
    }
}
