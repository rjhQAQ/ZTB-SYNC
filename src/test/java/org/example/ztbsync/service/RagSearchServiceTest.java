package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.ztbsync.api.RagSearchRequest;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.embedding.BgeRerankClient;
import org.example.ztbsync.embedding.ElasticsearchVectorQueryClient;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.rag.RagFileTypeFilter;
import org.example.ztbsync.rag.RagHybridRanker;
import org.example.ztbsync.rag.RagSearchHit;
import org.junit.jupiter.api.Test;

class RagSearchServiceTest {

    @Test
    void embedsQuestionSearchesEsAndMapsMatchedChunks() {
        ZtbProperties properties = new ZtbProperties();
        properties.getRagSearch().setDefaultTopK(3);
        properties.getRerank().setEnabled(false);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorQueryClient queryClient = mock(ElasticsearchVectorQueryClient.class);
        RagSearchService service = new RagSearchService(
                properties, embeddingClient, queryClient, new RagHybridRanker(properties), mock(BgeRerankClient.class));
        List<Double> vector = List.of(0.1, 0.2, 0.3);

        when(embeddingClient.embed(List.of("项目经理资质要求是什么"))).thenReturn(List.of(vector));
        when(queryClient.vectorSearch("project-1", vector, 50, RagFileTypeFilter.ALL)).thenReturn(List.of(
                new RagSearchHit("file-1", "招标文件.docx", "TENDER", 2,
                        "项目经理须具备相关资质。", "第三章 > 资格要求", 0.87654)));
        when(queryClient.keywordSearch("project-1", "项目经理资质要求是什么", 50, RagFileTypeFilter.ALL)).thenReturn(List.of());

        var response = service.search(new RagSearchRequest("project-1", "项目经理资质要求是什么", null));

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().projectId()).isEqualTo("project-1");
        assertThat(response.data().matchedDocuments()).singleElement()
                .satisfies(document -> {
                    assertThat(document.docId()).isEqualTo("file-1");
                    assertThat(document.docType()).isEqualTo("招标文件");
                    assertThat(document.relevantChapters()).containsExactly("第三章", "资格要求");
                    assertThat(document.sourceText()).contains("项目经理");
                    assertThat(document.score()).isEqualTo(0.6);
                });
    }

    @Test
    void capsTopKByConfig() {
        ZtbProperties properties = new ZtbProperties();
        properties.getRagSearch().setDefaultTopK(3);
        properties.getRagSearch().setMaxTopK(5);
        properties.getRagSearch().setMode("VECTOR");
        properties.getRerank().setEnabled(false);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorQueryClient queryClient = mock(ElasticsearchVectorQueryClient.class);
        RagSearchService service = new RagSearchService(
                properties, embeddingClient, queryClient, new RagHybridRanker(properties), mock(BgeRerankClient.class));
        List<Double> vector = List.of(0.1, 0.2, 0.3);

        when(embeddingClient.embed(List.of("问题"))).thenReturn(List.of(vector));
        when(queryClient.vectorSearch("project-1", vector, 50, RagFileTypeFilter.ALL)).thenReturn(List.of(
                new RagSearchHit("file-2", "投标文件.docx", "BID", 1, "投标内容", "", 0.7)));

        service.search("project-1", "问题", 99);

        verify(queryClient).vectorSearch("project-1", vector, 50, RagFileTypeFilter.ALL);
    }

    @Test
    void throwsNoDataWhenEsHasNoHits() {
        ZtbProperties properties = new ZtbProperties();
        properties.getRerank().setEnabled(false);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorQueryClient queryClient = mock(ElasticsearchVectorQueryClient.class);
        RagSearchService service = new RagSearchService(
                properties, embeddingClient, queryClient, new RagHybridRanker(properties), mock(BgeRerankClient.class));
        List<Double> vector = List.of(0.1, 0.2, 0.3);

        when(embeddingClient.embed(List.of("问题"))).thenReturn(List.of(vector));
        when(queryClient.vectorSearch("project-1", vector, 50, RagFileTypeFilter.ALL)).thenReturn(List.of());
        when(queryClient.keywordSearch("project-1", "问题", 50, RagFileTypeFilter.ALL)).thenReturn(List.of());

        assertThatThrownBy(() -> service.search("project-1", "问题", null))
                .isInstanceOf(RagSearchNoDataException.class)
                .hasMessageContaining("尚未上传或解析");
    }

    @Test
    void fallsBackToHybridWhenRerankFails() {
        ZtbProperties properties = new ZtbProperties();
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorQueryClient queryClient = mock(ElasticsearchVectorQueryClient.class);
        BgeRerankClient rerankClient = mock(BgeRerankClient.class);
        RagSearchService service = new RagSearchService(
                properties, embeddingClient, queryClient, new RagHybridRanker(properties), rerankClient);
        List<Double> vector = List.of(0.1, 0.2, 0.3);
        List<RagSearchHit> vectorHits = List.of(new RagSearchHit(
                "file-1", "招标文件.docx", "TENDER", 1, "保证金要求", "第二章", 0.9));

        when(embeddingClient.embed(List.of("保证金"))).thenReturn(List.of(vector));
        when(queryClient.vectorSearch("project-1", vector, 50, RagFileTypeFilter.TENDER)).thenReturn(vectorHits);
        when(queryClient.keywordSearch("project-1", "保证金", 50, RagFileTypeFilter.TENDER)).thenReturn(List.of());
        when(rerankClient.rerank(eq("保证金"), any(), eq(3))).thenThrow(new IllegalStateException("rerank down"));

        var response = service.search("project-1", "保证金", null, "TENDER");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().matchedDocuments()).singleElement()
                .satisfies(document -> assertThat(document.sourceText()).isEqualTo("保证金要求"));
    }
}
