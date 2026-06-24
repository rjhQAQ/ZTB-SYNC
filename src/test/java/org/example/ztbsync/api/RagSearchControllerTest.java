package org.example.ztbsync.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.service.RagSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RagSearchControllerTest {

    @Test
    void searchesWithSnakeCaseRequestAndResponse() throws Exception {
        RagSearchService service = mock(RagSearchService.class);
        when(service.search(any(RagSearchRequest.class))).thenReturn(RagSearchResponse.success(
                "project-1",
                List.of(new RagMatchedDocument(
                        "file-1",
                        "招标文件",
                        "招标文件.docx",
                        List.of("第三章", "资格要求"),
                        "命中文本",
                        0.91))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RagSearchController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"project_id":"project-1","user_question":"项目经理资质","top_k":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.project_id").value("project-1"))
                .andExpect(jsonPath("$.data.matched_documents[0].doc_id").value("file-1"))
                .andExpect(jsonPath("$.data.matched_documents[0].source_text").value("命中文本"));
    }

    @Test
    void returnsDocumentStyle404ForNoData() throws Exception {
        RagSearchService service = mock(RagSearchService.class);
        when(service.search(any(RagSearchRequest.class))).thenThrow(new RagSearchNoDataException("该项目尚未上传或解析招投标文件，无法进行文档内容审计。"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RagSearchController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"project_id":"project-1","user_question":"问题"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("该项目尚未上传或解析招投标文件，无法进行文档内容审计。"));
    }

    @Test
    void returnsBadGatewayWhenSearchBackendFails() throws Exception {
        RagSearchService service = mock(RagSearchService.class);
        when(service.search(any(RagSearchRequest.class))).thenThrow(new RagSearchUnavailableException("RAG 查询服务不可用"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RagSearchController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"project_id":"project-1","user_question":"问题"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502));
    }
}
