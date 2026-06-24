package org.example.ztbsync.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.ztbsync.api.RagMatchedDocument;
import org.example.ztbsync.api.RagSearchResponse;
import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.service.RagSearchService;
import org.junit.jupiter.api.Test;

class RagMcpToolTest {

    @Test
    void delegatesSearchToRagSearchService() {
        RagSearchService service = mock(RagSearchService.class);
        RagSearchResponse expected = RagSearchResponse.success(
                "project-1",
                List.of(new RagMatchedDocument("file-1", "投标文件", "投标文件.docx", List.of(), "命中文本", 0.8)));
        when(service.search("project-1", "问题", 5, null)).thenReturn(expected);
        RagMcpTool tool = new RagMcpTool(service);

        RagSearchResponse response = tool.searchZtbProjectDocuments("project-1", "问题", 5);

        assertThat(response).isSameAs(expected);
        verify(service).search("project-1", "问题", 5, null);
    }

    @Test
    void returnsErrorPayloadForNoData() {
        RagSearchService service = mock(RagSearchService.class);
        when(service.search("project-1", "问题", null, null)).thenThrow(new RagSearchNoDataException("没有数据"));
        RagMcpTool tool = new RagMcpTool(service);

        RagSearchResponse response = tool.searchZtbProjectDocuments("project-1", "问题", null);

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("没有数据");
        assertThat(response.data()).isNull();
    }
}
