package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.example.ztbsync.api.UploadFileRequest;
import org.example.ztbsync.api.UploadFileResponse;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;

class FileUploadServiceTest {

    @Test
    void createsPendingTaskSupersedesOlderAndSchedulesWorker() {
        FileProcessingTaskMapper mapper = mock(FileProcessingTaskMapper.class);
        FileProcessingWorker worker = mock(FileProcessingWorker.class);
        TaskExecutor directExecutor = Runnable::run;
        FileUploadService service = new FileUploadService(mapper, worker, directExecutor);

        UploadFileResponse response = service.submit(new UploadFileRequest(
                "project-1", "file-1", "招标文件.docx", "招标文件"));

        ArgumentCaptor<FileProcessingTask> taskCaptor = ArgumentCaptor.forClass(FileProcessingTask.class);
        verify(mapper).insert(taskCaptor.capture());
        FileProcessingTask inserted = taskCaptor.getValue();
        assertThat(inserted.getStatus()).isEqualTo("PENDING");
        assertThat(inserted.getFileType()).isEqualTo("TENDER");
        assertThat(response.taskId()).isEqualTo(inserted.getTaskId());

        verify(mapper).markOlderActiveTasksSuperseded(
                eq("project-1"), eq("file-1"), eq("TENDER"), eq(inserted.getTaskId()), any(LocalDateTime.class));
        verify(worker).process(inserted.getTaskId());
    }

    @Test
    void rejectsNonDocxFile() {
        FileUploadService service = new FileUploadService(
                mock(FileProcessingTaskMapper.class),
                mock(FileProcessingWorker.class),
                Runnable::run);

        assertThatThrownBy(() -> service.submit(new UploadFileRequest(
                "project-1", "file-1", "投标文件.pdf", "BID")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DOCX");
    }
}
