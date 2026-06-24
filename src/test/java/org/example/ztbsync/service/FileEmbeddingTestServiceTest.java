package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.ztbsync.api.EmbeddingTestResponse;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.embedding.ElasticsearchVectorStore;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.rag.RagBlockType;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentBlock;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class FileEmbeddingTestServiceTest {

    @Test
    void indexesUploadedDocxDirectlyIntoEs() {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setBaseUrl("http://embedding-service");
        properties.getElasticsearch().setBaseUrl("http://es");
        properties.getElasticsearch().setIndexName("ztb_file_embedding");
        properties.getEmbedding().setVectorDims(3);
        properties.getEmbedding().setBatchSize(1);
        RagDocumentParser parser = mock(RagDocumentParser.class);
        RagChunker chunker = mock(RagChunker.class);
        RagEmbeddingTextBuilder textBuilder = new RagEmbeddingTextBuilder();
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorStore vectorStore = mock(ElasticsearchVectorStore.class);
        FileEmbeddingTestService service = new FileEmbeddingTestService(
                properties, parser, chunker, textBuilder, embeddingClient, vectorStore);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "投标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1, 2, 3});
        List<RagDocumentBlock> blocks = List.of(
                new RagDocumentBlock(0, RagBlockType.PARAGRAPH, "片段一", 0, 0, 3),
                new RagDocumentBlock(1, RagBlockType.PARAGRAPH, "片段二", 0, 4, 7));
        List<RagChunk> chunks = List.of(
                new RagChunk(0, "片段一", "第一章", List.of("PARAGRAPH"), false, 0, 3),
                new RagChunk(1, "片段二", "第一章", List.of("PARAGRAPH"), false, 4, 7));
        when(parser.parse(any())).thenReturn(blocks);
        when(chunker.chunk(blocks)).thenReturn(chunks);
        FileProcessingTask expectedTask = new FileProcessingTask();
        expectedTask.setProjectId("project-1");
        expectedTask.setFileId("file-1");
        expectedTask.setFileName("投标文件.docx");
        expectedTask.setFileType("BID");
        when(embeddingClient.embed(List.of(textBuilder.build(expectedTask, chunks.get(0)))))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));
        when(embeddingClient.embed(List.of(textBuilder.build(expectedTask, chunks.get(1)))))
                .thenReturn(List.of(List.of(0.4, 0.5, 0.6)));

        EmbeddingTestResponse response = service.index(file, "project-1", "file-1", null, "投标文件");

        ArgumentCaptor<FileProcessingTask> taskCaptor = ArgumentCaptor.forClass(FileProcessingTask.class);
        verify(vectorStore).replaceFileChunks(
                taskCaptor.capture(),
                eq(chunks),
                eq(List.of(List.of(0.1, 0.2, 0.3), List.of(0.4, 0.5, 0.6))));
        FileProcessingTask task = taskCaptor.getValue();
        assertThat(task.getTaskId()).startsWith("embedding-test-");
        assertThat(task.getProjectId()).isEqualTo("project-1");
        assertThat(task.getFileId()).isEqualTo("file-1");
        assertThat(task.getFileName()).isEqualTo("投标文件.docx");
        assertThat(task.getFileType()).isEqualTo("BID");
        verify(embeddingClient).embed(List.of(textBuilder.build(task, chunks.get(0))));
        verify(embeddingClient).embed(List.of(textBuilder.build(task, chunks.get(1))));
        assertThat(response.indexedSegments()).isEqualTo(2);
        assertThat(response.blockCount()).isEqualTo(2);
        assertThat(response.textChars()).isEqualTo(6);
    }

    @Test
    void rejectsNonDocxFile() {
        FileEmbeddingTestService service = new FileEmbeddingTestService(
                new ZtbProperties(),
                mock(RagDocumentParser.class),
                mock(RagChunker.class),
                new RagEmbeddingTextBuilder(),
                mock(EmbeddingClient.class),
                mock(ElasticsearchVectorStore.class));
        MockMultipartFile file = new MockMultipartFile("file", "投标文件.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.index(file, "project-1", "file-1", null, "BID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("仅支持 DOCX");
    }
}
