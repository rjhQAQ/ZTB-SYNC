package org.example.ztbsync.embedding;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
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
 * OpenAI 风格的 bge-rerank 调用客户端。
 */
@Component
public class BgeRerankClient {

    private final ZtbProperties properties;
    private final RestTemplate restTemplate;
    private final RagEmbeddingTextBuilder embeddingTextBuilder;

    public BgeRerankClient(
            ZtbProperties properties,
            @Qualifier("rerankRestTemplate") RestTemplate restTemplate,
            RagEmbeddingTextBuilder embeddingTextBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.embeddingTextBuilder = embeddingTextBuilder;
    }

    public List<RagSearchHit> rerank(String query, List<RagSearchHit> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (isBlank(properties.getRerank().getBaseUrl())) {
            throw new RagSearchUnavailableException("bge-rerank base-url 未配置");
        }
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri(),
                HttpMethod.POST,
                new HttpEntity<>(requestBody(query, candidates), headers()),
                JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RagSearchUnavailableException("bge-rerank 返回异常状态: " + response.getStatusCode());
        }
        List<RerankScore> scores = parseScores(response.getBody());
        if (scores.isEmpty()) {
            throw new RagSearchUnavailableException("bge-rerank 未返回有效重排结果");
        }
        return toHits(candidates, scores, topK);
    }

    private Map<String, Object> requestBody(String query, List<RagSearchHit> candidates) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getRerank().getModel());
        body.put("query", query);
        body.put("documents", candidates.stream()
                .map(embeddingTextBuilder::buildForSearchHit)
                .toList());
        return body;
    }

    private List<RerankScore> parseScores(JsonNode body) {
        JsonNode array = body == null ? null : body.path("results");
        if (array == null || !array.isArray()) {
            array = body == null ? null : body.path("data");
        }
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<RerankScore> scores = new ArrayList<>();
        for (JsonNode item : array) {
            int index = item.has("index") ? item.path("index").asInt(-1) : item.path("document_index").asInt(-1);
            double score = item.has("relevance_score")
                    ? item.path("relevance_score").asDouble(Double.NaN)
                    : item.path("score").asDouble(Double.NaN);
            if (index >= 0 && !Double.isNaN(score)) {
                scores.add(new RerankScore(index, score));
            }
        }
        return scores;
    }

    private List<RagSearchHit> toHits(List<RagSearchHit> candidates, List<RerankScore> scores, int topK) {
        List<RerankScore> valid = scores.stream()
                .filter(score -> score.index() < candidates.size())
                .sorted(Comparator.comparingDouble(RerankScore::score).reversed())
                .toList();
        ScoreNormalizer normalizer = ScoreNormalizer.from(valid);
        return valid.stream()
                .limit(Math.max(1, topK))
                .map(score -> candidates.get(score.index()).withScore(normalizer.normalize(score.score())))
                .toList();
    }

    private URI uri() {
        String base = properties.getRerank().getBaseUrl().endsWith("/")
                ? properties.getRerank().getBaseUrl().substring(0, properties.getRerank().getBaseUrl().length() - 1)
                : properties.getRerank().getBaseUrl();
        String endpoint = properties.getRerank().getEndpoint();
        String path = endpoint == null || endpoint.isBlank() ? "/v1/rerank" : endpoint;
        return URI.create(base + (path.startsWith("/") ? path : "/" + path));
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = properties.getRerank().getApiKey();
        if (!isBlank(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
        return headers;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RerankScore(int index, double score) {
    }

    private record ScoreNormalizer(double min, double max, boolean rawAlreadyNormalized) {

        static ScoreNormalizer from(List<RerankScore> scores) {
            double min = scores.stream().mapToDouble(RerankScore::score).min().orElse(0);
            double max = scores.stream().mapToDouble(RerankScore::score).max().orElse(1);
            return new ScoreNormalizer(min, max, min >= 0 && max <= 1);
        }

        double normalize(double score) {
            if (rawAlreadyNormalized) {
                return Math.max(0, Math.min(1, score));
            }
            if (max <= min) {
                return 1;
            }
            return Math.max(0, Math.min(1, (score - min) / (max - min)));
        }
    }
}
