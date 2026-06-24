package org.example.ztbsync.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.example.ztbsync.api.EmbeddingTestResponse;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.embedding.ElasticsearchVectorStore;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentBlock;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileEmbeddingTestService {

    private static final Logger log = LoggerFactory.getLogger(FileEmbeddingTestService.class);

    private final ZtbProperties properties;
    private final RagDocumentParser ragDocumentParser;
    private final RagChunker ragChunker;
    private final RagEmbeddingTextBuilder embeddingTextBuilder;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchVectorStore vectorStore;

    public FileEmbeddingTestService(
            ZtbProperties properties,
            RagDocumentParser ragDocumentParser,
            RagChunker ragChunker,
            RagEmbeddingTextBuilder embeddingTextBuilder,
            EmbeddingClient embeddingClient,
            ElasticsearchVectorStore vectorStore) {
        this.properties = properties;
        this.ragDocumentParser = ragDocumentParser;
        this.ragChunker = ragChunker;
        this.embeddingTextBuilder = embeddingTextBuilder;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 直接上传文件写 ES，主要用于联调 embedding 服务和 Elasticsearch mapping。
     */
    public EmbeddingTestResponse index(
            MultipartFile file,
            String projectId,
            String fileId,
            String fileNameValue,
            String typeValue) {
        validateFile(file);
        String projectIdValue = requireText(projectId, "projectId 不能为空");
        String fileIdValue = requireText(fileId, "fileId 不能为空");
        FileType fileType = FileType.from(typeValue);
        String fileName = normalizeFileName(file, fileNameValue);
        validateConfig();
        byte[] bytes = readBytes(file);
        String taskId = "embedding-test-" + UUID.randomUUID();

        List<RagDocumentBlock> blocks = ragDocumentParser.parse(bytes);
        List<RagChunk> chunks = ragChunker.chunk(blocks);
        FileProcessingTask task = task(taskId, projectIdValue, fileIdValue, fileName, fileType);
        List<List<Double>> embeddings = embedChunks(task, chunks);

        vectorStore.replaceFileChunks(task, chunks, embeddings);
        int textChars = blocks.stream()
                .map(RagDocumentBlock::text)
                .filter(text -> text != null)
                .mapToInt(String::length)
                .sum();
        log.info("Finished direct embedding test: taskId={}, projectId={}, fileId={}, type={}, blocks={}, chunks={}",
                taskId, projectIdValue, fileIdValue, fileType.code(), blocks.size(), chunks.size());

        return new EmbeddingTestResponse(
                taskId,
                projectIdValue,
                fileIdValue,
                fileName,
                fileType.code(),
                file.getSize(),
                textChars,
                blocks.size(),
                chunks.size(),
                properties.getElasticsearch().getIndexName(),
                properties.getEmbedding().getModel(),
                properties.getEmbedding().getVectorDims());
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

    private FileProcessingTask task(
            String taskId,
            String projectId,
            String fileId,
            String fileName,
            FileType fileType) {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        task.setFileId(fileId);
        task.setFileName(fileName);
        task.setFileType(fileType.code());
        return task;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }
    }

    private void validateConfig() {
        if (isBlank(properties.getEmbedding().getBaseUrl())) {
            throw new BadRequestException("请先配置 ZTB_EMBEDDING_BASE_URL");
        }
        if (isBlank(properties.getElasticsearch().getBaseUrl())) {
            throw new BadRequestException("请先配置 ZTB_ES_BASE_URL");
        }
    }

    private String normalizeFileName(MultipartFile file, String fileNameValue) {
        String fileName = fileNameValue == null || fileNameValue.isBlank()
                ? file.getOriginalFilename()
                : fileNameValue;
        fileName = requireText(fileName, "fileName 不能为空");
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new BadRequestException("embedding 测试接口仅支持 DOCX 文件");
        }
        return fileName;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("读取上传文件失败");
        }
    }
}
