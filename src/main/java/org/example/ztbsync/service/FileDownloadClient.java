package org.example.ztbsync.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FileDownloadClient {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadClient.class);
    private static final int MAX_REDIRECTS = 3;

    private final RestTemplate restTemplate;
    private final ZtbProperties properties;
    private final ObjectMapper objectMapper;

    public FileDownloadClient(RestTemplate restTemplate, ZtbProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public byte[] download(String fileId, String fileName, String projectId) {
        String template = properties.getFileService().getDownloadUrlTemplate();
        URI uri = expandUri(template, fileId, fileName, projectId);
        String requestJson = requestJson(fileId, fileName, projectId);
        log.info("Downloading file bytes by POST: projectId={}, fileId={}, fileName={}, url={}, bodyBytes={}",
                projectId, fileId, fileName, uri, requestJson.getBytes(StandardCharsets.UTF_8).length);
        log.debug("File download request JSON: {}", requestJson);
        byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);
        ResponseEntity<byte[]> response = postForBytes(uri, requestBytes);
        for (int redirectCount = 0; isRedirect(response.getStatusCode()) && redirectCount < MAX_REDIRECTS; redirectCount++) {
            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new IllegalStateException("文件下载接口返回重定向状态但没有 Location: " + response.getStatusCode());
            }
            URI redirectedUri = location.isAbsolute() ? location : uri.resolve(location);
            log.info("Following file download redirect: projectId={}, fileId={}, status={}, location={}",
                    projectId, fileId, response.getStatusCode(), redirectedUri);
            uri = redirectedUri;
            response = postForBytes(uri, requestBytes);
        }
        if (isRedirect(response.getStatusCode())) {
            throw new IllegalStateException("文件下载接口重定向次数过多: " + response.getStatusCode());
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("文件下载接口返回异常状态: "
                    + response.getStatusCode()
                    + "，响应内容: "
                    + responsePreview(response.getBody()));
        }
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalStateException("文件下载接口返回内容为空");
        }
        log.info("Downloaded file bytes: projectId={}, fileId={}, fileName={}, bytes={}",
                projectId, fileId, fileName, body.length);
        return body;
    }

    private ResponseEntity<byte[]> postForBytes(URI uri, byte[] requestBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        return restTemplate.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(requestBytes, headers),
                byte[].class);
    }

    private String requestJson(String fileId, String fileName, String projectId) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("projectId", safe(projectId));
        requestBody.put("fileId", fileId);
        requestBody.put("fileName", safe(fileName));
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("文件下载请求 JSON 序列化失败", exception);
        }
    }

    private boolean isRedirect(HttpStatusCode statusCode) {
        int value = statusCode.value();
        return value == 301 || value == 302 || value == 303 || value == 307 || value == 308;
    }

    private URI expandUri(String template, String fileId, String fileName, String projectId) {
        return UriComponentsBuilder.fromUriString(template)
                .buildAndExpand(uriVariables(fileId, fileName, projectId))
                .toUri();
    }

    private Map<String, Object> uriVariables(String fileId, String fileName, String projectId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("fileId", fileId);
        variables.put("fileName", safe(fileName));
        variables.put("projectId", safe(projectId));
        return variables;
    }

    private String responsePreview(byte[] body) {
        if (body == null || body.length == 0) {
            return "<empty>";
        }
        String text = new String(body, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
