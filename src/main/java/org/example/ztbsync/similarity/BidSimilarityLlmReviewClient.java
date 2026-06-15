package org.example.ztbsync.similarity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ztbsync.config.ZtbProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BidSimilarityLlmReviewClient {

    private static final String SYSTEM_PROMPT = """
            你是招投标雷同分析复核助手。只返回严格 JSON，不要 Markdown，不要解释。
            判断命中片段是否属于疑似投标文件雷同、是否主要来自招标文件模板、以及复核理由。
            """;

    private final ZtbProperties properties;

    public BidSimilarityLlmReviewClient(ZtbProperties properties) {
        this.properties = properties;
    }

    public String review(SimilarityDocument left, SimilarityDocument right, SimilarityComputationResult result) {
        ZtbProperties.Similarity similarity = properties.getSimilarity();
        ZtbProperties.Llm llm = properties.getLlm();
        if (!similarity.isLlmReviewEnabled() || result.score() < similarity.getLlmReviewMinScore()) {
            return null;
        }
        if (!llm.isEnabled() || isBlank(llm.getBaseUrl()) || isBlank(llm.getApiKey()) || isBlank(llm.getModel())) {
            return """
                    {"reviewStatus":"SKIPPED","reason":"LLM review enabled but ztb.llm is disabled or incomplete"}
                    """;
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", llm.getModel());
        requestBody.put("temperature", 0);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt(left, right, result))));

        JsonNode response = restClient(llm)
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
        String content = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content").asText(null);
        return isBlank(content) ? null : stripJsonFence(content);
    }

    private RestClient restClient(ZtbProperties.Llm llm) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(llm.getTimeout());
        requestFactory.setReadTimeout(llm.getTimeout());
        return RestClient.builder()
                .baseUrl(llm.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + llm.getApiKey())
                .build();
    }

    private String prompt(SimilarityDocument left, SimilarityDocument right, SimilarityComputationResult result) {
        return """
                请复核以下两份投标文件的雷同命中片段，返回 JSON：
                {
                  "reviewStatus": "CONFIRMED|LIKELY_TEMPLATE|UNCERTAIN",
                  "suspicious": true,
                  "reason": "简短中文理由"
                }

                左侧公司：%s
                右侧公司：%s
                规则得分：%.2f
                风险等级：%s
                命中片段：%s
                """.formatted(
                nullToEmpty(left.companyName()),
                nullToEmpty(right.companyName()),
                result.score(),
                result.riskLevel(),
                result.hitFragments());
    }

    private String stripJsonFence(String content) {
        String stripped = content.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("^```(?:json)?\\s*", "");
            stripped = stripped.replaceFirst("\\s*```$", "");
        }
        return stripped.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
