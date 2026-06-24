package org.example.ztbsync.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.example.ztbsync.service.RagReindexService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RagReindexControllerTest {

    @Test
    void reindexesProject() throws Exception {
        RagReindexService service = mock(RagReindexService.class);
        when(service.reindexProject("project-1")).thenReturn(new RagReindexResponse(
                "project-1",
                2,
                1,
                1,
                List.of(new RagReindexFailureResponse("file-1", "招标.docx", "TENDER", "download failed"))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RagReindexController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/rag/reindex/projects/project-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id").value("project-1"))
                .andExpect(jsonPath("$.total_files").value(2))
                .andExpect(jsonPath("$.failed_count").value(1))
                .andExpect(jsonPath("$.failures[0].file_id").value("file-1"));
    }
}
