package org.example.ztbsync.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.ztbsync.service.FileEmbeddingTestService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileEmbeddingTestControllerTest {

    @Test
    void embeddingTestAcceptsMultipartFileAndReturnsIndexSummary() throws Exception {
        FileEmbeddingTestService service = mock(FileEmbeddingTestService.class);
        when(service.index(any(), eq("project-1"), eq("file-1"), eq("投标文件.docx"), eq("BID")))
                .thenReturn(new EmbeddingTestResponse(
                        "embedding-test-task",
                        "project-1",
                        "file-1",
                        "投标文件.docx",
                        "BID",
                        12,
                        120,
                        3,
                        2,
                        "ztb_file_embedding",
                        "bge-base-zh-v1.5",
                        768));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileEmbeddingTestController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "投标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1, 2, 3});

        mvc.perform(multipart("/api/files/embedding-test")
                        .file(file)
                        .param("projectId", "project-1")
                        .param("fileId", "file-1")
                        .param("fileName", "投标文件.docx")
                        .param("type", "BID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("embedding-test-task"))
                .andExpect(jsonPath("$.indexName").value("ztb_file_embedding"))
                .andExpect(jsonPath("$.indexedSegments").value(2));
    }
}
