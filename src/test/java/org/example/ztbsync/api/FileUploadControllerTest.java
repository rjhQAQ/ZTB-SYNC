package org.example.ztbsync.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.service.FileTaskQueryService;
import org.example.ztbsync.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileUploadControllerTest {

    @Test
    void uploadReturnsAcceptedTask() throws Exception {
        FileUploadService uploadService = mock(FileUploadService.class);
        when(uploadService.submit(any())).thenReturn(new UploadFileResponse(
                "task-1", "PENDING", "project-1", "file-1", "招标文件.docx", "TENDER"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileUploadController(uploadService, mock(FileTaskQueryService.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/files/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","fileId":"file-1","fileName":"招标文件.docx","type":"TENDER"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").value("task-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void uploadReturnsBadRequestForServiceValidationError() throws Exception {
        FileUploadService uploadService = mock(FileUploadService.class);
        when(uploadService.submit(any())).thenThrow(new BadRequestException("v1 仅支持 DOCX 文件"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileUploadController(uploadService, mock(FileTaskQueryService.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/files/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","fileId":"file-1","fileName":"投标文件.pdf","type":"BID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
