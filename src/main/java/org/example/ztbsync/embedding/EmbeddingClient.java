package org.example.ztbsync.embedding;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ztbsync.config.ZtbProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EmbeddingClient {

    private final ZtbProperties properties;
    private final RestTemplate restTemplate;

    public EmbeddingClient(
            ZtbProperties properties,
            @Qualifier("embeddingRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public List<List<Double>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        ZtbProperties.Embedding embedding = properties.getEmbedding();
        if (isBlank(embedding.getBaseUrl())) {
            throw new IllegalStateException("Embedding 已启用，但 base-url 未配置");
        }
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", embedding.getModel());
        requestBody.put("input", texts);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!isBlank(embedding.getApiKey())) {
            headers.setBearerAuth(embedding.getApiKey());
        }
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                endpointUri(embedding.getBaseUrl(), embedding.getEndpoint()),
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Embedding 接口返回异常状态: " + response.getStatusCode());
        }
        return parseEmbeddings(response.getBody(), texts.size(), embedding.getVectorDims());
    }

    private List<List<Double>> parseEmbeddings(JsonNode body, int expectedCount, int expectedDims) {
        JsonNode data = body == null ? null : body.path("data");
        if (data == null || !data.isArray()) {
            throw new IllegalStateException("Embedding 接口未返回 data 数组");
        }
        List<List<Double>> vectors = new ArrayList<>();
        for (JsonNode item : data) {
            JsonNode embedding = item.path("embedding");
            if (!embedding.isArray()) {
                throw new IllegalStateException("Embedding 响应缺少 embedding 数组");
            }
            List<Double> vector = new ArrayList<>(embedding.size());
            for (JsonNode value : embedding) {
                vector.add(value.asDouble());
            }
            if (vector.size() != expectedDims) {
                throw new IllegalStateException("Embedding 向量维度不匹配: expected="
                        + expectedDims + ", actual=" + vector.size());
            }
            vectors.add(vector);
        }
        if (vectors.size() != expectedCount) {
            throw new IllegalStateException("Embedding 返回数量不匹配: expected="
                    + expectedCount + ", actual=" + vectors.size());
        }
        return vectors;
    }

    private URI endpointUri(String baseUrl, String endpoint) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return URI.create(normalizedBase + normalizedEndpoint);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
