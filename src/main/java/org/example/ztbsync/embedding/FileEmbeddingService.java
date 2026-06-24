package org.example.ztbsync.embedding;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentBlock;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(FileEmbeddingService.class);

    private final ZtbProperties properties;
    private final RagDocumentParser ragDocumentParser;
    private final RagChunker ragChunker;
    private final RagEmbeddingTextBuilder embeddingTextBuilder;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchVectorStore vectorStore;
    private final FileProcessingTaskMapper taskMapper;

    public FileEmbeddingService(
            ZtbProperties properties,
            RagDocumentParser ragDocumentParser,
            RagChunker ragChunker,
            RagEmbeddingTextBuilder embeddingTextBuilder,
            EmbeddingClient embeddingClient,
            ElasticsearchVectorStore vectorStore,
            FileProcessingTaskMapper taskMapper) {
        this.properties = properties;
        this.ragDocumentParser = ragDocumentParser;
        this.ragChunker = ragChunker;
        this.embeddingTextBuilder = embeddingTextBuilder;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.taskMapper = taskMapper;
    }

    public void indexFile(FileProcessingTask task, byte[] docxBytes, String plainText) {
        if (!properties.getEmbedding().isEnabled()) {
            taskMapper.markEmbeddingSkipped(task.getTaskId(), LocalDateTime.now());
            log.info("Skipped file embedding because it is disabled: taskId={}, projectId={}, fileId={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId());
            return;
        }
        taskMapper.markEmbeddingProcessing(task.getTaskId(), LocalDateTime.now());
        try {
            List<RagDocumentBlock> blocks = ragDocumentParser.parse(docxBytes);
            List<RagChunk> chunks = ragChunker.chunk(blocks);
            if (chunks.isEmpty()) {
                taskMapper.markEmbeddingSuccess(task.getTaskId(), 0, LocalDateTime.now());
                log.info("File embedding produced no chunks: taskId={}, projectId={}, fileId={}, textChars={}",
                        task.getTaskId(), task.getProjectId(), task.getFileId(), plainText == null ? 0 : plainText.length());
                return;
            }
            List<List<Double>> embeddings = embedChunks(task, chunks);
            vectorStore.replaceFileChunks(task, chunks, embeddings);
            taskMapper.markEmbeddingSuccess(task.getTaskId(), chunks.size(), LocalDateTime.now());
            log.info("Indexed file embedding chunks: taskId={}, projectId={}, fileId={}, chunks={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), chunks.size());
        } catch (Exception exception) {
            String message = rootMessage(exception);
            taskMapper.markEmbeddingFailed(task.getTaskId(), message, LocalDateTime.now());
            log.error("File embedding failed but main file task remains successful: taskId={}, projectId={}, fileId={}, message={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), message, exception);
        }
    }

    private List<List<Double>> embedChunks(FileProcessingTask task, List<RagChunk> chunks) {
        int batchSize = Math.max(1, properties.getEmbedding().getBatchSize());
        List<List<Double>> embeddings = new ArrayList<>(chunks.size());
        for (int start = 0; start < chunks.size(); start += batchSize) {
            int end = Math.min(chunks.size(), start + batchSize);
            List<String> texts = chunks.subList(start, end).stream()
                    .map(chunk -> embeddingTextBuilder.build(task, chunk))
                    .toList();
            embeddings.addAll(embeddingClient.embed(texts));
        }
        return embeddings;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
