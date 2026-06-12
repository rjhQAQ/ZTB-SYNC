package org.example.ztbsync.service;

import java.util.Map;

import org.example.ztbsync.config.ZtbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FileDownloadClient {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadClient.class);

    private final RestClient restClient;
    private final ZtbProperties properties;

    public FileDownloadClient(RestClient.Builder restClientBuilder, ZtbProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public byte[] download(String fileId) {
        String template = properties.getFileService().getDownloadUrlTemplate();
        log.info("Downloading file bytes: fileId={}, urlTemplate={}", fileId, template);
        ResponseEntity<byte[]> response = restClient
                .get()
                .uri(template, Map.of("fileId", fileId))
                .retrieve()
                .toEntity(byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalStateException("文件下载接口返回内容为空");
        }
        log.info("Downloaded file bytes: fileId={}, bytes={}", fileId, body.length);
        return body;
    }
}
