package org.example.ztbsync.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.ztbsync.service.FileDownloadClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileDownloadTestControllerTest {

    @Test
    void downloadTestReturnsDownloadedBytes() throws Exception {
        FileDownloadClient fileDownloadClient = mock(FileDownloadClient.class);
        when(fileDownloadClient.download("file-1", "投标文件.docx", "project-1"))
                .thenReturn(new byte[] {1, 2, 3});
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileDownloadTestController(fileDownloadClient))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/files/download-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","fileId":"file-1","fileName":"投标文件.docx"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));

        verify(fileDownloadClient).download("file-1", "投标文件.docx", "project-1");
    }

    @Test
    void downloadTestRejectsMissingFileId() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileDownloadTestController(mock(FileDownloadClient.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/files/download-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","fileName":"投标文件.docx"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
