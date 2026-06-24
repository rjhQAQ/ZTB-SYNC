package org.example.ztbsync.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.example.ztbsync.api.RagReindexFailureResponse;
import org.example.ztbsync.api.RagReindexResponse;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.domain.ProjectBidderCompany;
import org.example.ztbsync.domain.ProjectInfo;
import org.example.ztbsync.embedding.ElasticsearchVectorStore;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.mapper.ProjectBidderCompanyMapper;
import org.example.ztbsync.mapper.ProjectInfoMapper;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagChunker;
import org.example.ztbsync.rag.RagDocumentBlock;
import org.example.ztbsync.rag.RagDocumentParser;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 项目级 RAG v2 索引重建服务，不依赖全文快照表，按 fileId 重新下载文件。
 */
@Service
public class RagReindexService {

    private static final Logger log = LoggerFactory.getLogger(RagReindexService.class);

    private final ZtbProperties properties;
    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectBidderCompanyMapper bidderCompanyMapper;
    private final FileDownloadClient fileDownloadClient;
    private final RagDocumentParser ragDocumentParser;
    private final RagChunker ragChunker;
    private final RagEmbeddingTextBuilder embeddingTextBuilder;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchVectorStore vectorStore;

    public RagReindexService(
            ZtbProperties properties,
            ProjectInfoMapper projectInfoMapper,
            ProjectBidderCompanyMapper bidderCompanyMapper,
            FileDownloadClient fileDownloadClient,
            RagDocumentParser ragDocumentParser,
            RagChunker ragChunker,
            RagEmbeddingTextBuilder embeddingTextBuilder,
            EmbeddingClient embeddingClient,
            ElasticsearchVectorStore vectorStore) {
        this.properties = properties;
        this.projectInfoMapper = projectInfoMapper;
        this.bidderCompanyMapper = bidderCompanyMapper;
        this.fileDownloadClient = fileDownloadClient;
        this.ragDocumentParser = ragDocumentParser;
        this.ragChunker = ragChunker;
        this.embeddingTextBuilder = embeddingTextBuilder;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public RagReindexResponse reindexProject(String projectId) {
        String projectIdValue = requireText(projectId, "projectId 不能为空");
        validateConfig();
        List<ReindexFile> files = projectFiles(projectIdValue);
        int successCount = 0;
        List<RagReindexFailureResponse> failures = new ArrayList<>();
        for (ReindexFile file : files) {
            try {
                reindexFile(file);
                successCount++;
            } catch (Exception exception) {
                String message = rootMessage(exception);
                failures.add(new RagReindexFailureResponse(file.fileId(), file.fileName(), file.fileType().code(), message));
                log.warn("RAG reindex file failed: projectId={}, fileId={}, type={}, message={}",
                        file.projectId(), file.fileId(), file.fileType().code(), message, exception);
            }
        }
        log.info("RAG project reindex finished: projectId={}, total={}, success={}, failed={}",
                projectIdValue, files.size(), successCount, failures.size());
        return new RagReindexResponse(projectIdValue, files.size(), successCount, failures.size(), failures);
    }

    private void reindexFile(ReindexFile file) {
        byte[] bytes = fileDownloadClient.download(file.fileId(), file.fileName(), file.projectId());
        List<RagDocumentBlock> blocks = ragDocumentParser.parse(bytes);
        List<RagChunk> chunks = ragChunker.chunk(blocks);
        FileProcessingTask task = task(file);
        List<List<Double>> embeddings = embedChunks(task, chunks);
        vectorStore.replaceFileChunks(task, chunks, embeddings);
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

    private List<ReindexFile> projectFiles(String projectId) {
        Map<String, ReindexFile> files = new LinkedHashMap<>();
        for (ProjectInfo tender : projectInfoMapper.findByProjectId(projectId)) {
            add(files, new ReindexFile(projectId, tender.getFileId(), tender.getFileName(), FileType.TENDER));
        }
        for (ProjectBidderCompany bidder : bidderCompanyMapper.findByProjectId(projectId)) {
            add(files, new ReindexFile(projectId, bidder.getFileId(), bidder.getFileName(), FileType.BID));
        }
        return new ArrayList<>(files.values());
    }

    private void add(Map<String, ReindexFile> files, ReindexFile file) {
        if (file.fileId() == null || file.fileId().isBlank()) {
            return;
        }
        files.putIfAbsent(file.fileType().code() + "|" + file.fileId(), file);
    }

    private FileProcessingTask task(ReindexFile file) {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId("rag-reindex-" + UUID.randomUUID());
        task.setProjectId(file.projectId());
        task.setFileId(file.fileId());
        task.setFileName(file.fileName());
        task.setFileType(file.fileType().code());
        return task;
    }

    private void validateConfig() {
        if (isBlank(properties.getEmbedding().getBaseUrl())) {
            throw new BadRequestException("请先配置 ZTB_EMBEDDING_BASE_URL");
        }
        if (isBlank(properties.getElasticsearch().getBaseUrl())) {
            throw new BadRequestException("请先配置 ZTB_ES_BASE_URL");
        }
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

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record ReindexFile(String projectId, String fileId, String fileName, FileType fileType) {
    }
}
