package org.example.ztbsync.embedding;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ElasticsearchVectorStore {

    private final ZtbProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagEmbeddingTextBuilder embeddingTextBuilder;
    private final AtomicBoolean indexChecked = new AtomicBoolean();

    public ElasticsearchVectorStore(
            ZtbProperties properties,
            @Qualifier("elasticsearchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            RagEmbeddingTextBuilder embeddingTextBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.embeddingTextBuilder = embeddingTextBuilder;
    }

    public void replaceFileChunks(
            FileProcessingTask task,
            List<RagChunk> chunks,
            List<List<Double>> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalStateException("Chunk 数量和向量数量不一致");
        }
        ensureIndex();
        deleteOldChunks(task);
        if (!chunks.isEmpty()) {
            bulkIndex(task, chunks, embeddings);
        }
    }

    private void ensureIndex() {
        ZtbProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        if (!elasticsearch.isAutoCreateIndex() || indexChecked.get()) {
            return;
        }
        ResponseEntity<String> exists = exchange(
                HttpMethod.HEAD,
                "/" + elasticsearch.getIndexName(),
                null,
                String.class);
        if (exists.getStatusCode().value() == 404) {
            ResponseEntity<String> created = exchange(
                    HttpMethod.PUT,
                    "/" + elasticsearch.getIndexName(),
                    createIndexBody(),
                    String.class);
            if (!created.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("创建 ES embedding 索引失败: " + created.getStatusCode());
            }
        } else if (!exists.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("检查 ES embedding 索引失败: " + exists.getStatusCode());
        }
        indexChecked.set(true);
    }

    private void deleteOldChunks(FileProcessingTask task) {
        Map<String, Object> query = Map.of(
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(
                                        term("projectId", task.getProjectId()),
                                        term("fileId", task.getFileId()),
                                        term("fileType", task.getFileType())))));
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/" + properties.getElasticsearch().getIndexName() + "/_delete_by_query?conflicts=proceed",
                query,
                String.class);
        if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode().value() != 404) {
            throw new IllegalStateException("删除旧 ES embedding chunk 失败: " + response.getStatusCode());
        }
    }

    private void bulkIndex(FileProcessingTask task, List<RagChunk> chunks, List<List<Double>> embeddings) {
        StringBuilder ndjson = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            ndjson.append(writeJson(Map.of("index", Map.of(
                    "_index", properties.getElasticsearch().getIndexName(),
                    "_id", documentId(task, chunk)))))
                    .append('\n');
            ndjson.append(writeJson(document(task, chunk, embeddings.get(i)))).append('\n');
        }
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.valueOf("application/x-ndjson"));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri("/_bulk?refresh=true"),
                HttpMethod.POST,
                new HttpEntity<>(ndjson.toString(), headers),
                JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("ES bulk 写入 embedding 失败: " + response.getStatusCode());
        }
        JsonNode body = response.getBody();
        if (body != null && body.path("errors").asBoolean(false)) {
            throw new IllegalStateException("ES bulk 写入 embedding 存在失败项");
        }
    }

    private Map<String, Object> document(FileProcessingTask task, RagChunk chunk, List<Double> embedding) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("projectId", task.getProjectId());
        document.put("fileId", task.getFileId());
        document.put("fileName", task.getFileName());
        document.put("fileType", task.getFileType());
        document.put("taskId", task.getTaskId());
        document.put("chunkIndex", chunk.chunkIndex());
        document.put("chunkText", chunk.chunkText());
        document.put("sectionPath", chunk.sectionPath());
        document.put("sectionPathText", chunk.sectionPath());
        document.put("blockTypes", chunk.blockTypes());
        document.put("boilerplate", chunk.boilerplate());
        document.put("charStart", chunk.charStart());
        document.put("charEnd", chunk.charEnd());
        document.put("model", properties.getEmbedding().getModel());
        document.put("embeddingText", embeddingTextBuilder.build(task, chunk));
        document.put("embedding", embedding);
        document.put("createdAt", Instant.now().toString());
        return document;
    }

    private Map<String, Object> createIndexBody() {
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("projectId", Map.of("type", "keyword"));
        propertiesMap.put("fileId", Map.of("type", "keyword"));
        propertiesMap.put("fileName", Map.of("type", "keyword"));
        propertiesMap.put("fileType", Map.of("type", "keyword"));
        propertiesMap.put("taskId", Map.of("type", "keyword"));
        propertiesMap.put("chunkIndex", Map.of("type", "integer"));
        propertiesMap.put("chunkText", textMapping());
        propertiesMap.put("sectionPath", Map.of("type", "keyword"));
        propertiesMap.put("sectionPathText", textMapping());
        propertiesMap.put("blockTypes", Map.of("type", "keyword"));
        propertiesMap.put("boilerplate", Map.of("type", "boolean"));
        propertiesMap.put("charStart", Map.of("type", "integer"));
        propertiesMap.put("charEnd", Map.of("type", "integer"));
        propertiesMap.put("model", Map.of("type", "keyword"));
        propertiesMap.put("embeddingText", textMapping());
        propertiesMap.put("embedding", Map.of(
                "type", "dense_vector",
                "dims", properties.getEmbedding().getVectorDims()));
        propertiesMap.put("createdAt", Map.of("type", "date"));
        return Map.of("mappings", Map.of("properties", propertiesMap));
    }

    private Map<String, Object> textMapping() {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", "text");
        if (!isBlank(properties.getElasticsearch().getChunkAnalyzer())) {
            mapping.put("analyzer", properties.getElasticsearch().getChunkAnalyzer());
        }
        if (!isBlank(properties.getElasticsearch().getChunkSearchAnalyzer())) {
            mapping.put("search_analyzer", properties.getElasticsearch().getChunkSearchAnalyzer());
        }
        return mapping;
    }

    private Map<String, Object> term(String field, String value) {
        return Map.of("term", Map.of(field, value));
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(uri(path), method, new HttpEntity<>(body, headers()), responseType);
    }

    private URI uri(String path) {
        ZtbProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        if (isBlank(elasticsearch.getBaseUrl())) {
            throw new IllegalStateException("Elasticsearch base-url 未配置");
        }
        String normalizedBase = elasticsearch.getBaseUrl().endsWith("/")
                ? elasticsearch.getBaseUrl().substring(0, elasticsearch.getBaseUrl().length() - 1)
                : elasticsearch.getBaseUrl();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(normalizedBase + normalizedPath);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ZtbProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        if (!isBlank(elasticsearch.getUsername())) {
            headers.setBasicAuth(elasticsearch.getUsername(), elasticsearch.getPassword() == null ? "" : elasticsearch.getPassword());
        }
        return headers;
    }

    private String documentId(FileProcessingTask task, RagChunk chunk) {
        return sanitize(task.getProjectId())
                + "_" + sanitize(task.getFileId())
                + "_" + sanitize(task.getFileType())
                + "_" + sanitize(task.getTaskId())
                + "_" + chunk.chunkIndex();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ES bulk JSON 序列化失败", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
