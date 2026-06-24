package org.example.ztbsync.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import org.example.ztbsync.api.RagMatchedDocument;
import org.example.ztbsync.api.RagSearchRequest;
import org.example.ztbsync.api.RagSearchResponse;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.embedding.BgeRerankClient;
import org.example.ztbsync.embedding.ElasticsearchVectorQueryClient;
import org.example.ztbsync.embedding.EmbeddingClient;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.rag.RagFileTypeFilter;
import org.example.ztbsync.rag.RagHybridRanker;
import org.example.ztbsync.rag.RagSearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RagSearchService {

    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);
    private static final String NO_DATA_MESSAGE = "该项目尚未上传或解析招投标文件，无法进行文档内容审计。";

    private final ZtbProperties properties;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchVectorQueryClient vectorQueryClient;
    private final RagHybridRanker hybridRanker;
    private final BgeRerankClient rerankClient;

    public RagSearchService(
            ZtbProperties properties,
            EmbeddingClient embeddingClient,
            ElasticsearchVectorQueryClient vectorQueryClient,
            RagHybridRanker hybridRanker,
            BgeRerankClient rerankClient) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.vectorQueryClient = vectorQueryClient;
        this.hybridRanker = hybridRanker;
        this.rerankClient = rerankClient;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        return search(
                request == null ? null : request.projectId(),
                request == null ? null : request.userQuestion(),
                request == null ? null : request.topK(),
                request == null ? null : request.fileType());
    }

    public RagSearchResponse search(String projectId, String userQuestion, Integer topK) {
        return search(projectId, userQuestion, topK, null);
    }

    public RagSearchResponse search(String projectId, String userQuestion, Integer topK, String fileType) {
        String projectIdValue = requireText(projectId, "project_id 不能为空");
        String questionValue = requireText(userQuestion, "user_question 不能为空");
        int topKValue = normalizeTopK(topK);
        RagFileTypeFilter fileTypeFilter = fileTypeFilter(fileType);

        try {
            List<RagSearchHit> candidates = recall(projectIdValue, questionValue, fileTypeFilter, topKValue);
            List<RagSearchHit> hits = rerankOrFallback(questionValue, candidates, topKValue).stream()
                    .filter(hit -> hit.score() >= properties.getRagSearch().getMinScore())
                    .limit(topKValue)
                    .toList();
            if (hits.isEmpty()) {
                throw new RagSearchNoDataException(NO_DATA_MESSAGE);
            }
            log.info("RAG search completed: projectId={}, topK={}, fileType={}, hits={}",
                    projectIdValue, topKValue, fileTypeFilter.name(), hits.size());
            return RagSearchResponse.success(projectIdValue, hits.stream()
                    .map(this::toMatchedDocument)
                    .toList());
        } catch (RagSearchNoDataException exception) {
            throw exception;
        } catch (RagSearchUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RagSearchUnavailableException("RAG 查询服务不可用: " + rootMessage(exception), exception);
        }
    }

    public String noDataMessage() {
        return NO_DATA_MESSAGE;
    }

    private List<RagSearchHit> recall(
            String projectId,
            String question,
            RagFileTypeFilter fileTypeFilter,
            int topK) {
        String mode = properties.getRagSearch().getMode() == null
                ? "HYBRID"
                : properties.getRagSearch().getMode().trim().toUpperCase();
        return switch (mode) {
            case "VECTOR" -> vectorRecall(projectId, question, fileTypeFilter, Math.max(topK, vectorCandidateSize()));
            case "KEYWORD" -> keywordRecall(projectId, question, fileTypeFilter, Math.max(topK, keywordCandidateSize()));
            default -> hybridRecall(projectId, question, fileTypeFilter, candidateLimit(topK));
        };
    }

    private List<RagSearchHit> hybridRecall(
            String projectId,
            String question,
            RagFileTypeFilter fileTypeFilter,
            int limit) {
        List<Double> queryVector = embedQuestion(question);
        List<RagSearchHit> vectorHits = vectorQueryClient.vectorSearch(
                projectId,
                queryVector,
                vectorCandidateSize(),
                fileTypeFilter);
        List<RagSearchHit> keywordHits = vectorQueryClient.keywordSearch(
                projectId,
                question,
                keywordCandidateSize(),
                fileTypeFilter);
        return hybridRanker.rank(vectorHits, keywordHits, limit);
    }

    private List<RagSearchHit> vectorRecall(
            String projectId,
            String question,
            RagFileTypeFilter fileTypeFilter,
            int limit) {
        return vectorQueryClient.vectorSearch(projectId, embedQuestion(question), limit, fileTypeFilter);
    }

    private List<RagSearchHit> keywordRecall(
            String projectId,
            String question,
            RagFileTypeFilter fileTypeFilter,
            int limit) {
        return vectorQueryClient.keywordSearch(projectId, question, limit, fileTypeFilter);
    }

    private List<RagSearchHit> rerankOrFallback(String question, List<RagSearchHit> candidates, int topK) {
        if (candidates.isEmpty() || !properties.getRerank().isEnabled()) {
            return candidates.stream().limit(topK).toList();
        }
        int candidateSize = Math.max(topK, properties.getRerank().getCandidateSize());
        List<RagSearchHit> limitedCandidates = candidates.stream()
                .limit(candidateSize)
                .toList();
        try {
            return rerankClient.rerank(question, limitedCandidates, topK);
        } catch (Exception exception) {
            if (!properties.getRerank().isFallbackToHybrid()) {
                throw exception;
            }
            log.warn("bge-rerank failed, fallback to hybrid ranking: message={}", rootMessage(exception));
            return limitedCandidates.stream().limit(topK).toList();
        }
    }

    private List<Double> embedQuestion(String question) {
        List<List<Double>> embeddings = embeddingClient.embed(List.of(question));
        if (embeddings.size() != 1) {
            throw new RagSearchUnavailableException("Embedding 查询向量返回数量不正确");
        }
        return embeddings.get(0);
    }

    private int candidateLimit(int topK) {
        int limit = Math.max(vectorCandidateSize(), keywordCandidateSize());
        if (properties.getRerank().isEnabled()) {
            limit = Math.max(limit, properties.getRerank().getCandidateSize());
        }
        return Math.max(topK, limit);
    }

    private int vectorCandidateSize() {
        return Math.max(1, properties.getRagSearch().getVectorCandidateSize());
    }

    private int keywordCandidateSize() {
        return Math.max(1, properties.getRagSearch().getKeywordCandidateSize());
    }

    private RagMatchedDocument toMatchedDocument(RagSearchHit hit) {
        return new RagMatchedDocument(
                hit.fileId(),
                chineseFileType(hit.fileType()),
                hit.fileName(),
                sectionPath(hit.sectionPath()),
                hit.chunkText(),
                roundScore(hit.score()));
    }

    private List<String> sectionPath(String sectionPath) {
        if (sectionPath == null || sectionPath.isBlank()) {
            return List.of();
        }
        return Arrays.stream(sectionPath.split("\\s*>\\s*"))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String chineseFileType(String fileType) {
        try {
            return switch (FileType.from(fileType)) {
                case TENDER -> "招标文件";
                case BID -> "投标文件";
            };
        } catch (Exception exception) {
            return fileType == null || fileType.isBlank() ? "未知文件" : fileType;
        }
    }

    private int normalizeTopK(Integer topK) {
        int defaultTopK = Math.max(1, properties.getRagSearch().getDefaultTopK());
        int maxTopK = Math.max(defaultTopK, properties.getRagSearch().getMaxTopK());
        int value = topK == null ? defaultTopK : topK;
        if (value <= 0) {
            throw new BadRequestException("top_k 必须大于 0");
        }
        return Math.min(value, maxTopK);
    }

    private RagFileTypeFilter fileTypeFilter(String fileType) {
        try {
            return RagFileTypeFilter.from(fileType);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private double roundScore(double score) {
        return BigDecimal.valueOf(score)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
