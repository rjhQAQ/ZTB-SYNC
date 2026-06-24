package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.ProjectBidderCompany;
import org.example.ztbsync.domain.ProjectInfo;
import org.example.ztbsync.embedding.ElasticsearchVectorStore;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.mapper.ProjectBidderCompanyMapper;
import org.example.ztbsync.mapper.ProjectInfoMapper;
import org.example.ztbsync.rag.RagBlockType;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentBlock;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagReindexServiceTest {

    @Test
    void continuesWhenSingleFileReindexFails() {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setBaseUrl("http://embedding");
        properties.getElasticsearch().setBaseUrl("http://es");
        ProjectInfoMapper projectInfoMapper = mock(ProjectInfoMapper.class);
        ProjectBidderCompanyMapper bidderMapper = mock(ProjectBidderCompanyMapper.class);
        FileDownloadClient fileDownloadClient = mock(FileDownloadClient.class);
        RagDocumentParser parser = mock(RagDocumentParser.class);
        RagChunker chunker = mock(RagChunker.class);
        RagEmbeddingTextBuilder textBuilder = new RagEmbeddingTextBuilder();
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorStore vectorStore = mock(ElasticsearchVectorStore.class);
        RagReindexService service = new RagReindexService(
                properties,
                projectInfoMapper,
                bidderMapper,
                fileDownloadClient,
                parser,
                chunker,
                textBuilder,
                embeddingClient,
                vectorStore);

        when(projectInfoMapper.findByProjectId("project-1")).thenReturn(List.of(tender()));
        when(bidderMapper.findByProjectId("project-1")).thenReturn(List.of(bidder()));
        when(fileDownloadClient.download("tender-file", "招标.docx", "project-1"))
                .thenThrow(new IllegalStateException("download failed"));
        when(fileDownloadClient.download("bid-file", "投标.docx", "project-1")).thenReturn(new byte[] {1, 2, 3});
        List<RagDocumentBlock> blocks = List.of(new RagDocumentBlock(0, RagBlockType.PARAGRAPH, "投标正文", 0, 0, 4));
        List<RagChunk> chunks = List.of(new RagChunk(0, "投标正文", "第一章", List.of("PARAGRAPH"), false, 0, 4));
        when(parser.parse(any())).thenReturn(blocks);
        when(chunker.chunk(blocks)).thenReturn(chunks);
        when(embeddingClient.embed(any())).thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        var response = service.reindexProject("project-1");

        assertThat(response.totalFiles()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.failures()).singleElement()
                .satisfies(failure -> assertThat(failure.fileId()).isEqualTo("tender-file"));
        ArgumentCaptor<FileProcessingTask> taskCaptor = ArgumentCaptor.forClass(FileProcessingTask.class);
        verify(vectorStore).replaceFileChunks(taskCaptor.capture(), any(), any());
        assertThat(taskCaptor.getValue().getFileId()).isEqualTo("bid-file");
        assertThat(taskCaptor.getValue().getFileType()).isEqualTo("BID");
    }

    private ProjectInfo tender() {
        ProjectInfo info = new ProjectInfo();
        info.setProjectId("project-1");
        info.setFileId("tender-file");
        info.setFileName("招标.docx");
        return info;
    }

    private ProjectBidderCompany bidder() {
        ProjectBidderCompany company = new ProjectBidderCompany();
        company.setProjectId("project-1");
        company.setFileId("bid-file");
        company.setFileName("投标.docx");
        return company;
    }
}
