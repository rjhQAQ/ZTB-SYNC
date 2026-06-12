package org.example.ztbsync.llm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.ProjectManagementPerson;
import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.extraction.TimeNormalizer;
import org.example.ztbsync.extraction.TimePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LlmExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(LlmExtractionClient.class);

    private static final String SYSTEM_PROMPT = """
            你是招投标文件信息抽取助手。只返回严格 JSON，不要 Markdown，不要解释。
            字段无法确定时返回 null，数组字段无法确定时返回空数组。
            """;

    private final ZtbProperties properties;
    private final ObjectMapper objectMapper;
    private final TimeNormalizer timeNormalizer;

    public LlmExtractionClient(
            ZtbProperties properties,
            ObjectMapper objectMapper,
            TimeNormalizer timeNormalizer) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.timeNormalizer = timeNormalizer;
    }

    public LlmExtractionResult extract(FileType fileType, String text) {
        ZtbProperties.Llm llm = properties.getLlm();
        if (!llm.isEnabled()) {
            log.debug("Skip LLM extraction because it is disabled: type={}", fileType);
            return LlmExtractionResult.empty();
        }
        if (isBlank(llm.getBaseUrl()) || isBlank(llm.getApiKey()) || isBlank(llm.getModel())) {
            throw new IllegalStateException("LLM 已启用，但 base-url/api-key/model 配置不完整");
        }

        String inputText = truncate(text, llm.getMaxInputChars());
        String prompt = buildUserPrompt(fileType, inputText);
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", llm.getModel());
        requestBody.put("temperature", 0);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)));
        log.info("Calling LLM extraction: type={}, model={}, inputChars={}, truncated={}",
                fileType, llm.getModel(), inputText == null ? 0 : inputText.length(),
                text != null && inputText != null && text.length() > inputText.length());

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
        if (isBlank(content)) {
            throw new IllegalStateException("LLM 返回内容为空");
        }
        String rawJson = stripJsonFence(content);
        JsonNode extracted = readJson(rawJson);
        log.info("LLM extraction returned JSON: type={}, rawJsonChars={}", fileType, rawJson.length());
        return fileType == FileType.TENDER
                ? new LlmExtractionResult(mapTender(extracted), new BidExtraction(), rawJson)
                : new LlmExtractionResult(new TenderExtraction(), mapBid(extracted), rawJson);
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

    private String buildUserPrompt(FileType fileType, String text) {
        if (fileType == FileType.TENDER) {
            return """
                    请从以下招标文件文本抽取 JSON：
                    {
                      "tenderCompanyName": "招标企业名称",
                      "agencyName": "代理机构名称",
                      "projectName": "项目名称",
                      "bidSubmitStartTime": "投标文件递交开始时间，yyyy-MM-dd HH:mm:ss 或原文",
                      "bidSubmitEndTime": "投标文件递交结束/截止时间，yyyy-MM-dd HH:mm:ss 或原文",
                      "timePoints": [
                        {"label": "时间点名称", "originalText": "原文时间", "normalizedTime": "yyyy-MM-dd HH:mm:ss"}
                      ]
                    }
                    只抽取项目相关时间点，不抽取投标人或人员相关时间。

                    文件文本：
                    """ + text;
        }
        return """
                请从以下投标文件文本抽取 JSON：
                {
                  "bidCompanyName": "投标公司名称",
                  "bidderContactPhone": "投标人联系电话",
                  "registeredAddress": "注册地址",
                  "mailingAddress": "通信地址",
                  "projectManagementPersonnel": [
                    {"name": "姓名", "role": "岗位", "certificate": "证书或证号", "phone": "联系电话", "originalText": "原文"}
                  ]
                }
                不要抽取时间字段。

                文件文本：
                """ + text;
    }

    private TenderExtraction mapTender(JsonNode json) {
        TenderExtraction extraction = new TenderExtraction();
        extraction.setTenderCompanyName(text(json, "tenderCompanyName"));
        extraction.setAgencyName(text(json, "agencyName"));
        extraction.setProjectName(text(json, "projectName"));
        extraction.setBidSubmitStartTime(parseTime(text(json, "bidSubmitStartTime")));
        extraction.setBidSubmitEndTime(parseTime(text(json, "bidSubmitEndTime")));
        List<TimePoint> points = new ArrayList<>();
        JsonNode timePoints = json.path("timePoints");
        if (timePoints.isArray()) {
            for (JsonNode point : timePoints) {
                String normalized = text(point, "normalizedTime");
                LocalDateTime parsed = parseTime(normalized);
                if (parsed == null) {
                    parsed = parseTime(text(point, "originalText"));
                }
                if (parsed != null) {
                    points.add(new TimePoint(
                            text(point, "label"),
                            text(point, "originalText"),
                            timeNormalizer.format(parsed),
                            "LLM"));
                }
            }
        }
        extraction.setTimePoints(points);
        return extraction;
    }

    private BidExtraction mapBid(JsonNode json) {
        BidExtraction extraction = new BidExtraction();
        extraction.setBidCompanyName(text(json, "bidCompanyName"));
        extraction.setBidderContactPhone(text(json, "bidderContactPhone"));
        extraction.setRegisteredAddress(text(json, "registeredAddress"));
        extraction.setMailingAddress(text(json, "mailingAddress"));
        List<ProjectManagementPerson> personnel = new ArrayList<>();
        JsonNode array = json.path("projectManagementPersonnel");
        if (array.isArray()) {
            for (JsonNode item : array) {
                personnel.add(new ProjectManagementPerson(
                        text(item, "name"),
                        text(item, "role"),
                        text(item, "certificate"),
                        text(item, "phone"),
                        text(item, "originalText")));
            }
        }
        extraction.setProjectManagementPersonnel(personnel);
        return extraction;
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception exception) {
            throw new IllegalStateException("LLM 未返回合法 JSON", exception);
        }
    }

    private LocalDateTime parseTime(String value) {
        return timeNormalizer.parse(value).orElse(null);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return isBlank(text) ? null : text.trim();
    }

    private String stripJsonFence(String content) {
        String stripped = content.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("^```(?:json)?\\s*", "");
            stripped = stripped.replaceFirst("\\s*```$", "");
        }
        return stripped.trim();
    }

    private String truncate(String text, int maxChars) {
        if (text == null || maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
