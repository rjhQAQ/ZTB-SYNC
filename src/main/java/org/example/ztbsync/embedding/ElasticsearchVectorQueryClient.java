package org.example.ztbsync.embedding;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.rag.RagFileTypeFilter;
import org.example.ztbsync.rag.RagSearchHit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 面向 ES 7.9 dense_vector 的 RAG 查询客户端。
 */
@Component
public class ElasticsearchVectorQueryClient {

    private static final List<String> SOURCE_FIELDS = List.of(
            "fileId",
            "fileName",
            "fileType",
            "chunkIndex",
            "chunkText",
            "sectionPath");

    private final ZtbProperties properties;
    private final RestTemplate restTemplate;

    public ElasticsearchVectorQueryClient(
            ZtbProperties properties,
            @Qualifier("elasticsearchRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public List<RagSearchHit> search(String projectId, List<Double> queryVector, int topK) {
        return vectorSearch(projectId, queryVector, topK, RagFileTypeFilter.ALL);
    }

    public List<RagSearchHit> vectorSearch(
            String projectId,
            List<Double> queryVector,
            int candidateSize,
            RagFileTypeFilter fileTypeFilter) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri("/" + properties.getElasticsearch().getIndexName() + "/_search"),
                HttpMethod.POST,
                new HttpEntity<>(vectorRequestBody(projectId, queryVector, candidateSize, fileTypeFilter), headers()),
                JsonNode.class);
        return parseSearchResponse(response, true);
    }

    public List<RagSearchHit> keywordSearch(
            String projectId,
            String question,
            int candidateSize,
            RagFileTypeFilter fileTypeFilter) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri("/" + properties.getElasticsearch().getIndexName() + "/_search"),
                HttpMethod.POST,
                new HttpEntity<>(keywordRequestBody(projectId, question, candidateSize, fileTypeFilter), headers()),
                JsonNode.class);
        return parseSearchResponse(response, false);
    }

    private List<RagSearchHit> parseSearchResponse(ResponseEntity<JsonNode> response, boolean vectorScore) {
        if (response.getStatusCode().value() == 404) {
            return List.of();
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RagSearchUnavailableException("ES RAG 查询返回异常状态: " + response.getStatusCode());
        }
        return parseHits(response.getBody(), vectorScore);
    }

    private Map<String, Object> vectorRequestBody(
            String projectId,
            List<Double> queryVector,
            int candidateSize,
            RagFileTypeFilter fileTypeFilter) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", candidateSize);
        body.put("_source", SOURCE_FIELDS);
        double minScore = properties.getRagSearch().getMinScore();
        if (minScore > 0) {
            body.put("min_score", 1.0 + minScore);
        }
        body.put("query", Map.of(
                "script_score", Map.of(
                        "query", filteredQuery(projectId, fileTypeFilter),
                        "script", Map.of(
                                "source", "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                                "params", Map.of("queryVector", queryVector)))));
        return body;
    }

    private Map<String, Object> keywordRequestBody(
            String projectId,
            String question,
            int candidateSize,
            RagFileTypeFilter fileTypeFilter) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", candidateSize);
        body.put("_source", SOURCE_FIELDS);
        body.put("query", Map.of(
                "bool", Map.of(
                        "filter", filters(projectId, fileTypeFilter),
                        "must", List.of(Map.of(
                                "multi_match", Map.of(
                                        "query", question,
                                        "fields", List.of("chunkText^3", "sectionPathText^2", "embeddingText")))))));
        return body;
    }

    private Map<String, Object> filteredQuery(String projectId, RagFileTypeFilter fileTypeFilter) {
        return Map.of("bool", Map.of("filter", filters(projectId, fileTypeFilter)));
    }

    private List<Map<String, Object>> filters(String projectId, RagFileTypeFilter fileTypeFilter) {
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(term("projectId", projectId));
        filters.add(Map.of("exists", Map.of("field", "embedding")));
        if (!properties.getRagSearch().isIncludeBoilerplate()) {
            filters.add(term("boilerplate", false));
        }
        String model = properties.getEmbedding().getModel();
        if (properties.getRagSearch().isFilterModel() && model != null && !model.isBlank()) {
            filters.add(term("model", model));
        }
        if (fileTypeFilter != null && fileTypeFilter.hasFilter()) {
            filters.add(term("fileType", fileTypeFilter.fileType()));
        }
        return filters;
    }

    private List<RagSearchHit> parseHits(JsonNode body, boolean vectorScore) {
        JsonNode hits = body == null ? null : body.path("hits").path("hits");
        if (hits == null || !hits.isArray() || hits.isEmpty()) {
            return List.of();
        }
        List<RagSearchHit> results = new ArrayList<>();
        double maxScore = body.path("hits").path("max_score").asDouble(0);
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            double rawScore = hit.path("_score").asDouble(0);
            double score = vectorScore ? normalizeVectorScore(rawScore) : normalizeKeywordScore(rawScore, maxScore);
            results.add(new RagSearchHit(
                    text(source, "fileId"),
                    text(source, "fileName"),
                    text(source, "fileType"),
                    source.path("chunkIndex").asInt(-1),
                    text(source, "chunkText"),
                    text(source, "sectionPath"),
                    score));
        }
        return results;
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private double normalizeVectorScore(double rawScore) {
        double score = rawScore - 1.0;
        return Math.max(0, Math.min(1, score));
    }

    private double normalizeKeywordScore(double rawScore, double maxScore) {
        if (maxScore <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, rawScore / maxScore));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private URI uri(String path) {
        ZtbProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        if (elasticsearch.getBaseUrl() == null || elasticsearch.getBaseUrl().isBlank()) {
            throw new RagSearchUnavailableException("Elasticsearch base-url 未配置");
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
        if (elasticsearch.getUsername() != null && !elasticsearch.getUsername().isBlank()) {
            headers.setBasicAuth(elasticsearch.getUsername(),
                    elasticsearch.getPassword() == null ? "" : elasticsearch.getPassword());
        }
        return headers;
    }
}
