package org.example.ztbsync.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.node.NullNode;
import org.example.ztbsync.service.BidSimilarityQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BidSimilarityControllerTest {

    @Test
    void projectQueryReturnsSimilarityResults() throws Exception {
        BidSimilarityQueryService queryService = mock(BidSimilarityQueryService.class);
        when(queryService.findByProject(eq("project-1"), eq(70.0), eq("HIGH"), eq("SUCCESS"), eq(10), eq(0)))
                .thenReturn(List.of(response()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BidSimilarityController(queryService)).build();

        mvc.perform(get("/api/bid-similarity/projects/project-1/results")
                        .param("minScore", "70")
                        .param("riskLevel", "HIGH")
                        .param("status", "SUCCESS")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(91.2))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"));
    }

    @Test
    void fileQueryReturnsSimilarityResults() throws Exception {
        BidSimilarityQueryService queryService = mock(BidSimilarityQueryService.class);
        when(queryService.findByProjectAndFile(eq("project-1"), eq("file-a"), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(response()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BidSimilarityController(queryService)).build();

        mvc.perform(get("/api/bid-similarity/projects/project-1/files/file-a/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leftFileId").value("file-a"));
    }

    private BidSimilarityResultResponse response() {
        return new BidSimilarityResultResponse(
                "project-1",
                "tender-1",
                "招标.docx",
                "file-a",
                "A.docx",
                "甲公司",
                "file-b",
                "B.docx",
                "乙公司",
                91.2,
                "HIGH",
                "SUCCESS",
                NullNode.getInstance(),
                NullNode.getInstance(),
                null,
                "task-1",
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
