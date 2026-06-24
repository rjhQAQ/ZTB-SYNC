package org.example.ztbsync.embedding;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.extraction.TenderExtraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TenderProjectNameIndexService {

    private static final Logger log = LoggerFactory.getLogger(TenderProjectNameIndexService.class);

    private final ZtbProperties properties;
    private final RestTemplate restTemplate;
    private final AtomicBoolean indexChecked = new AtomicBoolean();

    public TenderProjectNameIndexService(
            ZtbProperties properties,
            @Qualifier("elasticsearchRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public void index(FileProcessingTask task, TenderExtraction extraction) {
        String projectName = extraction == null ? null : extraction.getProjectName();
        if (isBlank(projectName)) {
            log.info("Skip tender project name indexing because projectName is blank: taskId={}, projectId={}, fileId={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId());
            return;
        }
        ensureIndex();
        ResponseEntity<String> response = exchange(
                HttpMethod.PUT,
                "/" + elasticsearch().getProjectNameIndexName()
                        + "/_doc/" + documentId(task)
                        + "?refresh=true",
                document(task, extraction),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("写入招标项目名称 ES 索引失败: " + response.getStatusCode());
        }
        log.info("Indexed tender project name: taskId={}, projectId={}, fileId={}, projectName={}",
                task.getTaskId(), task.getProjectId(), task.getFileId(), projectName);
    }

    private void ensureIndex() {
        ZtbProperties.Elasticsearch elasticsearch = elasticsearch();
        if (!elasticsearch.isAutoCreateIndex() || indexChecked.get()) {
            return;
        }
        ResponseEntity<String> exists = exchange(
                HttpMethod.HEAD,
                "/" + elasticsearch.getProjectNameIndexName(),
                null,
                String.class);
        if (exists.getStatusCode().value() == 404) {
            ResponseEntity<String> created = exchange(
                    HttpMethod.PUT,
                    "/" + elasticsearch.getProjectNameIndexName(),
                    createIndexBody(),
                    String.class);
            if (!created.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("创建招标项目名称 ES 索引失败: " + created.getStatusCode());
            }
        } else if (!exists.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("检查招标项目名称 ES 索引失败: " + exists.getStatusCode());
        }
        indexChecked.set(true);
    }

    private Map<String, Object> createIndexBody() {
        ZtbProperties.Elasticsearch elasticsearch = elasticsearch();
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("projectId", Map.of("type", "keyword"));
        propertiesMap.put("fileId", Map.of("type", "keyword"));
        propertiesMap.put("fileName", Map.of("type", "keyword"));
        propertiesMap.put("taskId", Map.of("type", "keyword"));
        propertiesMap.put("projectName", Map.of(
                "type", "text",
                "analyzer", elasticsearch.getProjectNameAnalyzer(),
                "search_analyzer", elasticsearch.getProjectNameSearchAnalyzer()));
        propertiesMap.put("projectNameKeyword", Map.of("type", "keyword"));
        propertiesMap.put("tenderCompanyName", Map.of("type", "keyword"));
        propertiesMap.put("agencyName", Map.of("type", "keyword"));
        propertiesMap.put("createdAt", Map.of("type", "date"));
        propertiesMap.put("updatedAt", Map.of("type", "date"));
        return Map.of("mappings", Map.of("properties", propertiesMap));
    }

    private Map<String, Object> document(FileProcessingTask task, TenderExtraction extraction) {
        String now = Instant.now().toString();
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("projectId", task.getProjectId());
        document.put("fileId", task.getFileId());
        document.put("fileName", task.getFileName());
        document.put("taskId", task.getTaskId());
        document.put("projectName", extraction.getProjectName());
        document.put("projectNameKeyword", extraction.getProjectName());
        document.put("tenderCompanyName", extraction.getTenderCompanyName());
        document.put("agencyName", extraction.getAgencyName());
        document.put("createdAt", now);
        document.put("updatedAt", now);
        return document;
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(uri(path), method, new HttpEntity<>(body, headers()), responseType);
    }

    private URI uri(String path) {
        ZtbProperties.Elasticsearch elasticsearch = elasticsearch();
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
        ZtbProperties.Elasticsearch elasticsearch = elasticsearch();
        if (!isBlank(elasticsearch.getUsername())) {
            headers.setBasicAuth(elasticsearch.getUsername(), elasticsearch.getPassword() == null ? "" : elasticsearch.getPassword());
        }
        return headers;
    }

    private ZtbProperties.Elasticsearch elasticsearch() {
        return properties.getElasticsearch();
    }

    private String documentId(FileProcessingTask task) {
        return sanitize(task.getProjectId()) + "_" + sanitize(task.getFileId());
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
