package org.example.ztbsync.embedding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.junit.jupiter.api.Test;

class FileEmbeddingServiceTest {

    @Test
    void marksSkippedWhenEmbeddingIsDisabled() {
        ZtbProperties properties = new ZtbProperties();
        RagDocumentParser parser = mock(RagDocumentParser.class);
        RagChunker chunker = mock(RagChunker.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorStore vectorStore = mock(ElasticsearchVectorStore.class);
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        FileEmbeddingService service = new FileEmbeddingService(
                properties, parser, chunker, new RagEmbeddingTextBuilder(), embeddingClient, vectorStore, taskMapper);

        service.indexFile(task(), new byte[] {1}, "正文");

        verify(taskMapper).markEmbeddingSkipped(eq("task-1"), any(LocalDateTime.class));
        verify(embeddingClient, never()).embed(any());
        verify(vectorStore, never()).replaceFileChunks(any(), any(), any());
    }

    @Test
    void writesChunksWhenEmbeddingSucceeds() {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setEnabled(true);
        properties.getEmbedding().setBatchSize(1);
        RagDocumentParser parser = mock(RagDocumentParser.class);
        RagChunker chunker = mock(RagChunker.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorStore vectorStore = mock(ElasticsearchVectorStore.class);
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        RagEmbeddingTextBuilder textBuilder = new RagEmbeddingTextBuilder();
        FileEmbeddingService service = new FileEmbeddingService(
                properties, parser, chunker, textBuilder, embeddingClient, vectorStore, taskMapper);
        List<RagChunk> chunks = List.of(
                new RagChunk(0, "片段一", "第一章", List.of("PARAGRAPH"), false, 0, 3),
                new RagChunk(1, "片段二", "第一章", List.of("PARAGRAPH"), false, 4, 7));
        when(parser.parse(any())).thenReturn(List.of());
        when(chunker.chunk(any())).thenReturn(chunks);
        FileProcessingTask task = task();
        when(embeddingClient.embed(List.of(textBuilder.build(task, chunks.get(0))))).thenReturn(List.of(List.of(0.1, 0.2)));
        when(embeddingClient.embed(List.of(textBuilder.build(task, chunks.get(1))))).thenReturn(List.of(List.of(0.3, 0.4)));

        service.indexFile(task, new byte[] {1}, "正文");

        verify(taskMapper).markEmbeddingProcessing(eq("task-1"), any(LocalDateTime.class));
        verify(vectorStore).replaceFileChunks(eq(task), eq(chunks), eq(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4))));
        verify(taskMapper).markEmbeddingSuccess(eq("task-1"), eq(2), any(LocalDateTime.class));
    }

    @Test
    void marksFailedAndSwallowsExceptionWhenEmbeddingFails() {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setEnabled(true);
        RagDocumentParser parser = mock(RagDocumentParser.class);
        RagChunker chunker = mock(RagChunker.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ElasticsearchVectorStore vectorStore = mock(ElasticsearchVectorStore.class);
        FileProcessingTaskMapper taskMapper = mock(FileProcessingTaskMapper.class);
        RagEmbeddingTextBuilder textBuilder = new RagEmbeddingTextBuilder();
        FileEmbeddingService service = new FileEmbeddingService(
                properties, parser, chunker, textBuilder, embeddingClient, vectorStore, taskMapper);
        List<RagChunk> chunks = List.of(new RagChunk(0, "片段一", "第一章", List.of("PARAGRAPH"), false, 0, 3));
        when(parser.parse(any())).thenReturn(List.of());
        when(chunker.chunk(any())).thenReturn(chunks);
        when(embeddingClient.embed(List.of(textBuilder.build(task(), chunks.get(0))))).thenThrow(new IllegalStateException("embedding down"));

        service.indexFile(task(), new byte[] {1}, "正文");

        verify(taskMapper).markEmbeddingFailed(eq("task-1"), eq("embedding down"), any(LocalDateTime.class));
        verify(vectorStore, never()).replaceFileChunks(any(), any(), any());
    }

    private FileProcessingTask task() {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId("task-1");
        task.setProjectId("project-1");
        task.setFileId("file-1");
        task.setFileName("投标文件.docx");
        task.setFileType("BID");
        return task;
    }
}
