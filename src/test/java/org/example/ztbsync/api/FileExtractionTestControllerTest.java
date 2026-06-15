package org.example.ztbsync.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.service.FileExtractionTestService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileExtractionTestControllerTest {

    @Test
    void extractTestAcceptsMultipartFileAndReturnsExtraction() throws Exception {
        FileExtractionTestService service = mock(FileExtractionTestService.class);
        TenderExtraction merged = new TenderExtraction();
        merged.setProjectName("智慧园区建设项目");
        when(service.extract(any(), eq("TENDER"))).thenReturn(new ExtractionTestResponse(
                "招标文件.docx",
                "TENDER",
                12,
                120,
                null,
                new TenderExtraction(),
                new TenderExtraction(),
                merged));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileExtractionTestController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "招标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1, 2, 3});

        mvc.perform(multipart("/api/files/extract-test")
                        .file(file)
                        .param("type", "TENDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("招标文件.docx"))
                .andExpect(jsonPath("$.type").value("TENDER"))
                .andExpect(jsonPath("$.mergedResult.projectName").value("智慧园区建设项目"));
    }
}
