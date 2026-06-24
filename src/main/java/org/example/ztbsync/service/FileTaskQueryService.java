package org.example.ztbsync.service;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.example.ztbsync.api.TaskDetailResponse;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.exception.ResourceNotFoundException;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class FileTaskQueryService {

    private final FileProcessingTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public FileTaskQueryService(FileProcessingTaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    public TaskDetailResponse getTask(String taskId) {
        FileProcessingTask task = taskMapper.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在: " + taskId);
        }
        return toResponse(task);
    }

    public TaskDetailResponse getLatestTask(String projectId, String fileId, String type) {
        FileType fileType = FileType.from(type);
        List<FileProcessingTask> tasks = taskMapper.findByBusinessKey(projectId, fileId, fileType.code());
        if (tasks.isEmpty()) {
            throw new ResourceNotFoundException("未找到该文件的处理任务");
        }
        return toResponse(tasks.get(0));
    }

    private TaskDetailResponse toResponse(FileProcessingTask task) {
        return new TaskDetailResponse(
                task.getTaskId(),
                task.getProjectId(),
                task.getFileId(),
                task.getFileName(),
                task.getFileType(),
                task.getStatus(),
                task.getErrorMessage(),
                parseJson(task.getResultSummaryJson()),
                task.getEmbeddingStatus(),
                task.getEmbeddingErrorMessage(),
                task.getEmbeddingIndexedSegments(),
                task.getEmbeddingUpdatedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getStartedAt(),
                task.getFinishedAt());
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return TextNode.valueOf(value);
        }
    }
}
